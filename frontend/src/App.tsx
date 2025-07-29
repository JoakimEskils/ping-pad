import { useState } from 'react';
import LoginForm from './components/LoginForm';
import Dashboard from './components/Dashboard';
import type { User } from './types';

function App() {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const handleLogin = async (email: string, password: string) => {
    setIsLoading(true);
    try {
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // Mock user data
      const mockUser: User = {
        id: '1',
        email,
        name: email.split('@')[0],
        apiKeys: ['mock-api-key-123']
      };
      
      setUser(mockUser);
      // Show success message
      alert('Login successful!');
    } catch (error) {
      alert('Login failed. Please check your credentials.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogout = () => {
    setUser(null);
    alert('Logged out successfully!');
  };

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
          </p>
        </div>
        <LoginForm onLogin={handleLogin} isLoading={isLoading} />
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
