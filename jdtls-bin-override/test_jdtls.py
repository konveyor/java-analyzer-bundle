import os
import pytest
from unittest.mock import patch
from jdtls import get_proxy_jvm_args


class TestGetProxyJvmArgs:
    """Test suite for get_proxy_jvm_args function."""

    def test_no_proxy_variables(self):
        """Test with no proxy environment variables set."""
        with patch.dict(os.environ, {}, clear=True):
            result = get_proxy_jvm_args()
            assert result == []

    def test_http_proxy_with_hostname_only(self):
        """Test HTTP_PROXY with just hostname."""
        with patch.dict(os.environ, {'HTTP_PROXY': 'http://proxy.example.com'}, clear=True):
            result = get_proxy_jvm_args()
            assert '-Dhttp.proxyHost=proxy.example.com' in result
            assert '-Dhttp.proxyPort=80' in result

    def test_http_proxy_with_port(self):
        """Test HTTP_PROXY with hostname and port."""
        with patch.dict(os.environ, {'HTTP_PROXY': 'http://proxy.example.com:8080'}, clear=True):
            result = get_proxy_jvm_args()
            assert '-Dhttp.proxyHost=proxy.example.com' in result
            assert '-Dhttp.proxyPort=8080' in result

    def test_http_proxy_lowercase(self):
        """Test http_proxy (lowercase) environment variable."""
        with patch.dict(os.environ, {'http_proxy': 'http://proxy.example.com:3128'}, clear=True):
            result = get_proxy_jvm_args()
            assert '-Dhttp.proxyHost=proxy.example.com' in result
            assert '-Dhttp.proxyPort=3128' in result

    def test_http_proxy_with_credentials(self):
        """Test HTTP_PROXY with username and password."""
        with patch.dict(os.environ, {'HTTP_PROXY': 'http://user:pass@proxy.example.com:8080'}, clear=True):
            result = get_proxy_jvm_args()
            assert '-Dhttp.proxyHost=proxy.example.com' in result
            assert '-Dhttp.proxyPort=8080' in result
            assert '-Dhttp.proxyUser=user' in result
            assert '-Dhttp.proxyPassword=pass' in result

    def test_https_proxy_with_hostname_only(self):
        """Test HTTPS_PROXY with just hostname."""
        with patch.dict(os.environ, {'HTTPS_PROXY': 'https://secure-proxy.example.com'}, clear=True):
            result = get_proxy_jvm_args()
            assert '-Dhttps.proxyHost=secure-proxy.example.com' in result
            assert '-Dhttps.proxyPort=443' in result

    def test_https_proxy_with_port(self):
        """Test HTTPS_PROXY with hostname and port."""
        with patch.dict(os.environ, {'HTTPS_PROXY': 'https://secure-proxy.example.com:8443'}, clear=True):
            result = get_proxy_jvm_args()
            assert '-Dhttps.proxyHost=secure-proxy.example.com' in result
            assert '-Dhttps.proxyPort=8443' in result

    def test_https_proxy_lowercase(self):
        """Test https_proxy (lowercase) environment variable."""
        with patch.dict(os.environ, {'https_proxy': 'https://secure-proxy.example.com:8443'}, clear=True):
            result = get_proxy_jvm_args()
            assert '-Dhttps.proxyHost=secure-proxy.example.com' in result
            assert '-Dhttps.proxyPort=8443' in result

    def test_https_proxy_with_credentials(self):
        """Test HTTPS_PROXY with username and password."""
        with patch.dict(os.environ, {'HTTPS_PROXY': 'https://user:pass@secure-proxy.example.com:8443'}, clear=True):
            result = get_proxy_jvm_args()
            assert '-Dhttps.proxyHost=secure-proxy.example.com' in result
            assert '-Dhttps.proxyPort=8443' in result
            assert '-Dhttps.proxyUser=user' in result
            assert '-Dhttps.proxyPassword=pass' in result

    def test_no_proxy_single_host(self):
        """Test NO_PROXY with a single host."""
        with patch.dict(os.environ, {'NO_PROXY': 'localhost'}, clear=True):
            result = get_proxy_jvm_args()
            assert '-Dhttp.nonProxyHosts=localhost' in result

    def test_no_proxy_multiple_hosts(self):
        """Test NO_PROXY with comma-separated hosts."""
        with patch.dict(os.environ, {'NO_PROXY': 'localhost,127.0.0.1,.example.com'}, clear=True):
            result = get_proxy_jvm_args()
            assert '-Dhttp.nonProxyHosts=localhost|127.0.0.1|.example.com' in result

    def test_no_proxy_lowercase(self):
        """Test no_proxy (lowercase) environment variable."""
        with patch.dict(os.environ, {'no_proxy': 'localhost,127.0.0.1'}, clear=True):
            result = get_proxy_jvm_args()
            assert '-Dhttp.nonProxyHosts=localhost|127.0.0.1' in result

    def test_all_proxies_combined(self):
        """Test with all proxy environment variables set."""
        env_vars = {
            'HTTP_PROXY': 'http://user:pass@proxy.example.com:8080',
            'HTTPS_PROXY': 'https://user:pass@secure-proxy.example.com:8443',
            'NO_PROXY': 'localhost,127.0.0.1,.local'
        }
        with patch.dict(os.environ, env_vars, clear=True):
            result = get_proxy_jvm_args()

            # HTTP proxy assertions
            assert '-Dhttp.proxyHost=proxy.example.com' in result
            assert '-Dhttp.proxyPort=8080' in result
            assert '-Dhttp.proxyUser=user' in result
            assert '-Dhttp.proxyPassword=pass' in result

            # HTTPS proxy assertions
            assert '-Dhttps.proxyHost=secure-proxy.example.com' in result
            assert '-Dhttps.proxyPort=8443' in result
            assert '-Dhttps.proxyUser=user' in result
            assert '-Dhttps.proxyPassword=pass' in result

            # NO_PROXY assertions
            assert '-Dhttp.nonProxyHosts=localhost|127.0.0.1|.local' in result

    def test_uppercase_takes_precedence(self):
        """Test that uppercase environment variables take precedence over lowercase."""
        env_vars = {
            'HTTP_PROXY': 'http://upper.example.com:8080',
            'http_proxy': 'http://lower.example.com:3128'
        }
        with patch.dict(os.environ, env_vars, clear=True):
            result = get_proxy_jvm_args()
            assert '-Dhttp.proxyHost=upper.example.com' in result
            assert '-Dhttp.proxyPort=8080' in result

    def test_proxy_without_scheme(self):
        """Test proxy URL without scheme (should add default scheme)."""
        with patch.dict(os.environ, {'HTTP_PROXY': 'proxy.example.com:8080'}, clear=True):
            result = get_proxy_jvm_args()
            assert '-Dhttp.proxyHost=proxy.example.com' in result
            assert '-Dhttp.proxyPort=8080' in result

    def test_https_proxy_without_scheme(self):
        """Test HTTPS proxy URL without scheme (should add default https scheme)."""
        with patch.dict(os.environ, {'HTTPS_PROXY': 'secure-proxy.example.com:8443'}, clear=True):
            result = get_proxy_jvm_args()
            assert '-Dhttps.proxyHost=secure-proxy.example.com' in result
            assert '-Dhttps.proxyPort=8443' in result

    def test_proxy_with_username_no_password(self):
        """Test proxy with username but no password."""
        with patch.dict(os.environ, {'HTTP_PROXY': 'http://user@proxy.example.com:8080'}, clear=True):
            result = get_proxy_jvm_args()
            assert '-Dhttp.proxyHost=proxy.example.com' in result
            assert '-Dhttp.proxyPort=8080' in result
            assert '-Dhttp.proxyUser=user' in result
            # Password should not be in result since it's None
            assert not any('proxyPassword' in arg for arg in result)

    def test_http_proxy_no_port_non_http_scheme(self):
        """Test HTTP proxy without port and non-http scheme doesn't add default port."""
        with patch.dict(os.environ, {'HTTP_PROXY': 'socks://proxy.example.com'}, clear=True):
            result = get_proxy_jvm_args()
            assert '-Dhttp.proxyHost=proxy.example.com' in result
            # Should not add default port for non-http scheme
            assert not any('proxyPort' in arg for arg in result)

    def test_https_proxy_no_port_non_https_scheme(self):
        """Test HTTPS proxy without port and non-https scheme doesn't add default port."""
        with patch.dict(os.environ, {'HTTPS_PROXY': 'socks://proxy.example.com'}, clear=True):
            result = get_proxy_jvm_args()
            assert '-Dhttps.proxyHost=proxy.example.com' in result
            # Should not add default port for non-https scheme
            assert not any('proxyPort' in arg for arg in result)

    def test_empty_proxy_value(self):
        """Test with empty proxy environment variable."""
        with patch.dict(os.environ, {'HTTP_PROXY': ''}, clear=True):
            result = get_proxy_jvm_args()
            assert result == []

    def test_invalid_url_no_hostname(self):
        """Test with invalid URL that has no hostname."""
        with patch.dict(os.environ, {'HTTP_PROXY': 'http://'}, clear=True):
            result = get_proxy_jvm_args()
            assert result == []
