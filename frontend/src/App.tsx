import { useState, useEffect } from 'react';
import Dashboard from './components/Dashboard';
import LoginForm from './components/LoginForm';
import type { User } from './types';
import { getToken, removeToken, getAuthHeaders } from './utils/auth';

function App() {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    checkAuthStatus();
  }, []);

  const checkAuthStatus = async () => {
    try {
      const token = getToken();
      if (!token) {
        setIsLoading(false);
        return;
      }

      const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';
      const response = await fetch(`${backendUrl}/api/user/me`, {
        headers: getAuthHeaders(),
        credentials: 'include'
      });
      
      if (response.ok) {
        const userData = await response.json();
        if (userData) {
          setUser({
            id: userData.id.toString(),
            email: userData.email,
            name: userData.name,
            apiKeys: []
          });
        }
      } else if (response.status === 401) {
        // Token invalid, remove it
        removeToken();
        setUser(null);
      }
    } catch (error) {
      console.error('Auth check failed:', error);
      removeToken();
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogout = async () => {
    try {
      const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';
      await fetch(`${backendUrl}/api/auth/logout`, {
        method: 'POST',
        headers: getAuthHeaders(),
        credentials: 'include'
      });
    } catch (error) {
      console.error('Logout failed:', error);
    }
    removeToken();
    setUser(null);
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-screen bg-gradient-to-br from-slate-50 to-slate-100">
        <div className="text-slate-600">Loading...</div>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-50 to-slate-100 px-4">
        <div className="w-full max-w-md space-y-8">
          <div className="text-center">
            <h1 className="text-4xl font-bold bg-gradient-to-r from-blue-600 to-blue-800 bg-clip-text text-transparent mb-4">
              PingPad
            </h1>
            <p className="text-lg text-slate-600 mb-8">
              API Testing & Monitoring Dashboard
            </p>
          </div>
          
          <LoginForm onLoginSuccess={checkAuthStatus} />
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100">
      <Dashboard user={user} onLogout={handleLogout} />
    </div>
  );
}

export default App;
