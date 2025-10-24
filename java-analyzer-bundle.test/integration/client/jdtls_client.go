package client

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"time"

	jsonrpc2 "github.com/konveyor/analyzer-lsp/jsonrpc2_v2"
	"github.com/konveyor/analyzer-lsp/lsp/protocol"
	"github.com/sirupsen/logrus"
)

// JDTLSClient manages communication with JDT.LS server
type JDTLSClient struct {
	jdtlsPath    string
	workspaceDir string
	conn         *jsonrpc2.Connection
	cmd          *exec.Cmd
	dialer       *CmdDialer
	ctx          context.Context
	cancel       context.CancelFunc
	logger       *logrus.Logger
}

// CmdDialer wraps a command's stdin/stdout as an io.ReadWriteCloser
// This is based on github.com/konveyor/analyzer-lsp/lsp/base_service_client/cmd_dialer.go
type CmdDialer struct {
	Cmd    *exec.Cmd
	Stdin  io.WriteCloser
	Stdout io.ReadCloser
	err    error
}

func (rwc *CmdDialer) Read(p []byte) (int, error) {
	if rwc.err != nil {
		return 0, fmt.Errorf("cannot read: %w", rwc.err)
	}
	return rwc.Stdout.Read(p)
}

func (rwc *CmdDialer) Write(p []byte) (int, error) {
	if rwc.err != nil {
		return 0, fmt.Errorf("cannot write: %w", rwc.err)
	}
	return rwc.Stdin.Write(p)
}

func (rwc *CmdDialer) Close() error {
	if rwc.Cmd.Process != nil {
		rwc.Cmd.Process.Kill()
	}
	return rwc.Cmd.Wait()
}

func (rwc *CmdDialer) Dial(ctx context.Context) (io.ReadWriteCloser, error) {
	if rwc.err != nil {
		return rwc, fmt.Errorf("cannot dial: %w", rwc.err)
	}
	return rwc, nil
}

// HandlerFunc is a simple handler that can be used for logging
type HandlerFunc struct {
	logger *logrus.Logger
}

func (h *HandlerFunc) Handle(ctx context.Context, req *jsonrpc2.Request) (interface{}, error) {
	// Log incoming requests/notifications from server
	if h.logger != nil && req.Method != "" {
		h.logger.Debugf("Received from server: %s", req.Method)
	}
	// We don't handle any server requests in this client
	return nil, jsonrpc2.ErrNotHandled
}

// NewJDTLSClient creates a new JDT.LS client
func NewJDTLSClient(jdtlsPath, workspaceDir string) *JDTLSClient {
	logger := logrus.New()
	logger.SetLevel(logrus.InfoLevel)

	return &JDTLSClient{
		jdtlsPath:    jdtlsPath,
		workspaceDir: workspaceDir,
		logger:       logger,
	}
}

// Start launches the JDT.LS server process and establishes connection
func (c *JDTLSClient) Start() error {
	c.logger.Info("Starting JDT.LS server...")

	// Prepare workspace
	dataDir := filepath.Join(c.workspaceDir, ".metadata")
	if err := os.MkdirAll(dataDir, 0755); err != nil {
		return fmt.Errorf("failed to create data directory: %w", err)
	}

	// Determine config directory
	configDir := filepath.Join(c.jdtlsPath, "config_linux")

	// Build command
	jdtlsBin := filepath.Join(c.jdtlsPath, "bin", "jdtls")
	c.cmd = exec.Command(jdtlsBin,
		"-configuration", configDir,
		"-data", dataDir,
	)

	// Create pipes for stdin/stdout
	stdin, err := c.cmd.StdinPipe()
	if err != nil {
		return fmt.Errorf("failed to create stdin pipe: %w", err)
	}

	stdout, err := c.cmd.StdoutPipe()
	if err != nil {
		return fmt.Errorf("failed to create stdout pipe: %w", err)
	}

	// Capture stderr for debugging
	c.cmd.Stderr = os.Stderr

	// Create context for connection
	c.ctx, c.cancel = context.WithCancel(context.Background())

	// Create dialer
	c.dialer = &CmdDialer{
		Cmd:    c.cmd,
		Stdin:  stdin,
		Stdout: stdout,
	}

	// Start the process
	if err := c.cmd.Start(); err != nil {
		return fmt.Errorf("failed to start JDT.LS: %w", err)
	}

	c.logger.Infof("JDT.LS server started with PID %d", c.cmd.Process.Pid)

	// Create connection using jsonrpc2.Dial
	// The binder returns connection options with our handler
	binder := jsonrpc2.BinderFunc(func(ctx context.Context, conn *jsonrpc2.Connection) jsonrpc2.ConnectionOptions {
		return jsonrpc2.ConnectionOptions{
			Handler: &HandlerFunc{logger: c.logger},
		}
	})

	c.conn, err = jsonrpc2.Dial(c.ctx, c.dialer, binder)
	if err != nil {
		return fmt.Errorf("failed to establish JSON-RPC connection: %w", err)
	}

	c.logger.Info("JSON-RPC connection established")

	return nil
}

// Initialize sends the LSP initialize request
func (c *JDTLSClient) Initialize() (*protocol.InitializeResult, error) {
	c.logger.Info("Initializing LSP connection...")

	workspaceURI := protocol.DocumentURI("file://" + c.workspaceDir)

	params := struct {
		ProcessID             *int                                `json:"processId"`
		RootURI               *protocol.DocumentURI               `json:"rootUri"`
		Capabilities          protocol.ClientCapabilities         `json:"capabilities"`
		InitializationOptions map[string]interface{}              `json:"initializationOptions"`
	}{
		ProcessID: nil,
		RootURI:   &workspaceURI,
		Capabilities: protocol.ClientCapabilities{
			Workspace: &protocol.WorkspaceClientCapabilities{
				WorkspaceFolders: true,
				Configuration:    true,
			},
			TextDocument: &protocol.TextDocumentClientCapabilities{
				Synchronization: &protocol.TextDocumentSyncClientCapabilities{
					DidSave: true,
				},
			},
		},
		InitializationOptions: map[string]interface{}{
			"bundles": []string{
				"/jdtls/plugins/java-analyzer-bundle.core-1.0.0-SNAPSHOT.jar",
			},
			"workspaceFolders": []string{string(workspaceURI)},
		},
	}

	var result protocol.InitializeResult
	call := c.conn.Call(c.ctx, "initialize", params)
	if err := call.Await(c.ctx, &result); err != nil {
		return nil, fmt.Errorf("initialize request failed: %w", err)
	}

	// Send initialized notification
	if err := c.conn.Notify(c.ctx, "initialized", &protocol.InitializedParams{}); err != nil {
		return nil, fmt.Errorf("initialized notification failed: %w", err)
	}

	c.logger.Info("JDT.LS initialized successfully")

	// Wait for server to process projects
	time.Sleep(5 * time.Second)

	return &result, nil
}

// ExecuteCommand executes a workspace command
func (c *JDTLSClient) ExecuteCommand(command string, arguments []interface{}) (interface{}, error) {
	// Convert arguments to json.RawMessage
	var rawArgs []json.RawMessage
	for _, arg := range arguments {
		data, err := json.Marshal(arg)
		if err != nil {
			return nil, fmt.Errorf("failed to marshal argument: %w", err)
		}
		rawArgs = append(rawArgs, data)
	}

	params := protocol.ExecuteCommandParams{
		Command:   command,
		Arguments: rawArgs,
	}

	var result interface{}
	call := c.conn.Call(c.ctx, "workspace/executeCommand", params)
	if err := call.Await(c.ctx, &result); err != nil {
		return nil, fmt.Errorf("executeCommand failed: %w", err)
	}

	return result, nil
}

// SearchSymbols executes a symbol search using the analyzer bundle
func (c *JDTLSClient) SearchSymbols(project, query string, location int, analysisMode string, includedPaths []string) ([]protocol.SymbolInformation, error) {
	args := map[string]interface{}{
		"project":      project,
		"query":        query,
		"location":     fmt.Sprintf("%d", location),
		"analysisMode": analysisMode,
	}

	if includedPaths != nil {
		args["includedPaths"] = includedPaths
	}

	result, err := c.ExecuteCommand("io.konveyor.tackle.ruleEntry", []interface{}{args})
	if err != nil {
		return nil, err
	}

	// Convert result to SymbolInformation array
	symbols := []protocol.SymbolInformation{}

	// Result might be nil or empty
	if result == nil {
		return symbols, nil
	}

	// Marshal and unmarshal to convert interface{} to proper type
	data, err := json.Marshal(result)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal result: %w", err)
	}

	if err := json.Unmarshal(data, &symbols); err != nil {
		return nil, fmt.Errorf("failed to unmarshal symbols: %w", err)
	}

	return symbols, nil
}

// Shutdown gracefully shuts down the LSP connection
func (c *JDTLSClient) Shutdown() error {
	c.logger.Info("Shutting down JDT.LS server...")

	// Send shutdown request
	var result interface{}
	call := c.conn.Call(c.ctx, "shutdown", nil)
	if err := call.Await(c.ctx, &result); err != nil {
		c.logger.Warnf("Shutdown request failed: %v", err)
	}

	// Send exit notification
	if err := c.conn.Notify(c.ctx, "exit", nil); err != nil {
		c.logger.Warnf("Exit notification failed: %v", err)
	}

	// Close connection
	if err := c.conn.Close(); err != nil {
		c.logger.Warnf("Failed to close connection: %v", err)
	}

	// Cancel context
	c.cancel()

	// Wait for process with timeout
	done := make(chan error, 1)
	go func() {
		done <- c.cmd.Wait()
	}()

	select {
	case <-time.After(10 * time.Second):
		c.logger.Warn("JDT.LS did not exit cleanly, killing process")
		if err := c.cmd.Process.Kill(); err != nil {
			return fmt.Errorf("failed to kill process: %w", err)
		}
		return fmt.Errorf("process killed due to timeout")
	case err := <-done:
		if err != nil && err != io.EOF {
			c.logger.Warnf("Process exited with error: %v", err)
		}
	}

	c.logger.Info("JDT.LS server shut down")
	return nil
}

// Close is an alias for Shutdown
func (c *JDTLSClient) Close() error {
	return c.Shutdown()
}
