import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { setToken } from '../utils/auth';

interface LoginFormProps {
  onLoginSuccess: () => void;
}

export default function LoginForm({ onLoginSuccess }: LoginFormProps) {
  const navigate = useNavigate();
  const [isRegistering, setIsRegistering] = useState(false);
  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      if (isRegistering) {
        // Register new user
        const response = await fetch(`${backendUrl}/api/auth/register`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          credentials: 'include',
          body: JSON.stringify({ email, name, password }),
        });

        if (!response.ok) {
          const data = await response.json();
          throw new Error(data.error || 'Registration failed');
        }

        // After successful registration, automatically log in
        await handleLogin(email, password);
      } else {
        // Login
        await handleLogin(email, password);
      }
    } catch (err: any) {
      setError(err.message || 'An error occurred');
      setIsLoading(false);
    }
  };

  const handleLogin = async (loginEmail: string, loginPassword: string) => {
    const response = await fetch(`${backendUrl}/api/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({ email: loginEmail, password: loginPassword }),
    });

    if (!response.ok) {
      const data = await response.json().catch(() => ({}));
      throw new Error(data.error || 'Invalid email or password');
    }

    const data = await response.json();
    
    // Store JWT token
    if (data.token) {
      setToken(data.token);
    }

    // Login successful, check auth status and navigate
    onLoginSuccess();
    navigate('/dashboard');
  };

  const handleTestUserLogin = async () => {
    setIsLoading(true);
    setError('');

    try {
      const response = await fetch(`${backendUrl}/api/auth/login-as-test`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
      });

      if (!response.ok) {
        throw new Error('Failed to login as test user');
      }

      const data = await response.json();
      
      // Store JWT token
      if (data.token) {
        setToken(data.token);
      }

      // Login successful, check auth status
      onLoginSuccess();
    } catch (err: any) {
      setError(err.message || 'Failed to login as test user');
      setIsLoading(false);
    }
  };

  return (
    <Card className="w-full max-w-md">
      <CardHeader className="space-y-1">
        <CardTitle className="text-2xl font-bold text-center">
          {isRegistering ? 'Create Account' : 'Sign In'}
        </CardTitle>
        <CardDescription className="text-center">
          {isRegistering
            ? 'Create a new account to get started'
            : 'Enter your credentials to access your dashboard'}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          {isRegistering && (
            <div className="space-y-2">
              <Label htmlFor="name">Name</Label>
              <Input
                id="name"
                type="text"
                placeholder="John Doe"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
                disabled={isLoading}
              />
            </div>
          )}

          <div className="space-y-2">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              type="email"
              placeholder="you@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              disabled={isLoading}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="password">Password</Label>
            <Input
              id="password"
              type="password"
              placeholder={isRegistering ? 'At least 6 characters' : 'Enter your password'}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={isRegistering ? 6 : undefined}
              disabled={isLoading}
            />
          </div>

          {error && (
            <div className="p-3 text-sm text-red-600 bg-red-50 border border-red-200 rounded-md">
              {error}
            </div>
          )}

          <Button type="submit" className="w-full" disabled={isLoading}>
            {isLoading ? 'Please wait...' : isRegistering ? 'Create Account' : 'Sign In'}
          </Button>
        </form>

        <div className="mt-6">
          <div className="relative">
            <div className="absolute inset-0 flex items-center">
              <span className="w-full border-t" />
            </div>
            <div className="relative flex justify-center text-xs uppercase">
              <span className="bg-white px-2 text-slate-500">Or</span>
            </div>
          </div>

          <div className="mt-6 space-y-3">
            <Button
              type="button"
              variant="outline"
              className="w-full"
              onClick={handleTestUserLogin}
              disabled={isLoading}
            >
              Login as Test User
            </Button>

            <div className="text-center text-sm">
              <button
                type="button"
                onClick={() => {
                  setIsRegistering(!isRegistering);
                  setError('');
                  setEmail('');
                  setName('');
                  setPassword('');
                }}
                className="text-blue-600 hover:text-blue-700 hover:underline"
                disabled={isLoading}
              >
                {isRegistering
                  ? 'Already have an account? Sign in'
                  : "Don't have an account? Sign up"}
              </button>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
