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
	// Just close the pipes - don't kill or wait for the process
	// The Shutdown() method handles process termination
	if rwc.Stdin != nil {
		rwc.Stdin.Close()
	}
	if rwc.Stdout != nil {
		rwc.Stdout.Close()
	}
	return nil
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

func (h *HandlerFunc) Handle(ctx context.Context, req *jsonrpc2.Request) (any, error) {
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

	// Ensure workspace directory exists; JDT.LS will create ".metadata" under it
	if err := os.MkdirAll(c.workspaceDir, 0755); err != nil {
		return fmt.Errorf("failed to create workspace directory: %w", err)
	}

	// Determine config directory
	configDir := filepath.Join(c.jdtlsPath, "config_linux")

	// Build command
	jdtlsBin := filepath.Join(c.jdtlsPath, "bin", "jdtls")
	c.cmd = exec.Command(jdtlsBin,
		"-configuration", configDir,
		"-data", c.workspaceDir,
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
		ProcessID             *int                        `json:"processId"`
		RootURI               *protocol.DocumentURI       `json:"rootUri"`
		Capabilities          protocol.ClientCapabilities `json:"capabilities"`
		InitializationOptions map[string]any              `json:"initializationOptions"`
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
		InitializationOptions: map[string]any{
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
func (c *JDTLSClient) ExecuteCommand(command string, arguments []any) (any, error) {
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

	var result any
	call := c.conn.Call(c.ctx, "workspace/executeCommand", params)
	if err := call.Await(c.ctx, &result); err != nil {
		return nil, fmt.Errorf("executeCommand failed: %w", err)
	}

	return result, nil
}

// AnnotationElement represents an annotation element with name and value
type AnnotationElement struct {
	Name  string `json:"name"`
	Value string `json:"value"`
}

// AnnotationQuery represents query parameters for annotation searches
type AnnotationQuery struct {
	Pattern  string              `json:"pattern"`
	Elements []AnnotationElement `json:"elements"`
}

// SearchSymbols executes a symbol search using the analyzer bundle
func (c *JDTLSClient) SearchSymbols(project, query string, location int, analysisMode string, includedPaths []string) ([]protocol.SymbolInformation, error) {
	return c.SearchSymbolsWithAnnotation(project, query, location, analysisMode, includedPaths, nil)
}

// SearchSymbolsWithAnnotation executes a symbol search with optional annotation query
func (c *JDTLSClient) SearchSymbolsWithAnnotation(project, query string, location int, analysisMode string, includedPaths []string, annotationQuery *AnnotationQuery) ([]protocol.SymbolInformation, error) {
	args := map[string]any{
		"project":      project,
		"query":        query,
		"location":     fmt.Sprintf("%d", location),
		"analysisMode": analysisMode,
	}

	if includedPaths != nil {
		args["includedPaths"] = includedPaths
	}

	if annotationQuery != nil {
		args["annotationQuery"] = annotationQuery
	}

	result, err := c.ExecuteCommand("io.konveyor.tackle.ruleEntry", []any{args})
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

	// Guard against shutdown when connection was never established
	if c.conn == nil {
		c.logger.Warn("Connection was never established, skipping LSP shutdown")
		// Still clean up the process if it exists
		if c.cmd != nil && c.cmd.Process != nil {
			c.logger.Info("Terminating JDT.LS process...")
			c.cmd.Process.Kill()
		}
		if c.cancel != nil {
			c.cancel()
		}
		return nil
	}

	// Create a context with timeout for shutdown requests
	shutdownCtx, shutdownCancel := context.WithTimeout(c.ctx, 3*time.Second)
	defer shutdownCancel()

	// Send shutdown request
	var result any
	call := c.conn.Call(shutdownCtx, "shutdown", nil)
	if err := call.Await(shutdownCtx, &result); err != nil {
		c.logger.Warnf("Shutdown request failed: %v", err)
	}

	// Send exit notification
	if err := c.conn.Notify(shutdownCtx, "exit", nil); err != nil {
		c.logger.Warnf("Exit notification failed: %v", err)
	}

	// Close connection (this closes the pipes)
	if err := c.conn.Close(); err != nil {
		c.logger.Warnf("Failed to close connection: %v", err)
	}

	// Cancel main context
	c.cancel()

	// Wait for process to finish with a reasonable timeout
	// Use a channel to avoid blocking indefinitely
	done := make(chan error, 1)
	go func() {
		done <- c.cmd.Wait()
	}()

	select {
	case err := <-done:
		// Process exited
		if err != nil {
			// Don't treat this as an error - just log it
			c.logger.Debugf("Process exited with: %v", err)
		}
		c.logger.Info("JDT.LS server shut down cleanly")
	case <-time.After(2 * time.Second):
		// Process didn't exit in time, kill it
		c.logger.Info("JDT.LS did not exit in time, terminating...")
		if c.cmd.Process != nil {
			c.cmd.Process.Kill()
		}
		// Wait a bit more for the kill to take effect
		<-done
		c.logger.Info("JDT.LS server terminated")
	}

	return nil
}

// Close is an alias for Shutdown
func (c *JDTLSClient) Close() error {
	return c.Shutdown()
}
