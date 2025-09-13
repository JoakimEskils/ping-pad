import { useState, useEffect } from 'react';
import Dashboard from './components/Dashboard';
import type { User } from './types';

function App() {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // Check if user is logged in
    checkAuthStatus();
  }, []);

  const checkAuthStatus = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/user/me', {
        credentials: 'include'
      });
      
      if (response.ok) {
        const userData = await response.json();
        if (userData) {
          setUser({
            id: userData.id.toString(),
            email: userData.email,
            name: userData.name,
            apiKeys: ['github-oauth-key'] // Placeholder for OAuth users
          });
        }
      }
    } catch (error) {
      console.error('Auth check failed:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleGitHubLogin = () => {
    window.location.href = "http://localhost:8080/oauth2/authorization/github";
  };

  const handleLogout = async () => {
    try {
      await fetch('http://localhost:8080/logout', {
        method: 'POST',
        credentials: 'include'
      });
    } catch (error) {
      console.error('Logout failed:', error);
    }
    setUser(null);
    alert('Logged out successfully!');
  };

  if (isLoading) {
    return (
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: '100vh' 
      }}>
        <div>Loading...</div>
      </div>
    );
  }

  if (!user) {
    return (
      <div style={{ 
        maxWidth: '480px', 
        margin: '0 auto', 
        padding: '40px 20px' 
      }}>
        <div style={{ textAlign: 'center', marginBottom: '32px' }}>
          <h1 style={{ fontSize: '32px', fontWeight: 'bold', marginBottom: '16px' }}>PingPad</h1>
          <p style={{ fontSize: '18px', color: '#666' }}>
            Sign in with GitHub to access your API testing dashboard
          </p>
        </div>
        
        <div style={{ textAlign: 'center' }}>
          <button
            onClick={handleGitHubLogin}
            style={{
              padding: "0.75rem 1.5rem",
              fontSize: "1rem",
              backgroundColor: "#24292f",
              color: "#fff",
              border: "none",
              borderRadius: "5px",
              cursor: "pointer",
              display: "flex",
              alignItems: "center",
              gap: "8px",
              margin: "0 auto"
            }}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
            </svg>
            Login with GitHub
          </button>
        </div>
      </div>
    );
  }

  return (
    <div style={{ minHeight: '100vh', backgroundColor: '#f7fafc' }}>
      <Dashboard user={user} onLogout={handleLogout} />
    </div>
  );
}

export default App;
