import type { User } from '../types';
import ApiEndpoints from './ApiEndpoints';
import { Button } from './ui/button';
import { LogOut } from 'lucide-react';

interface DashboardProps {
  user: User;
  onLogout: () => void;
}

export default function Dashboard({ user, onLogout }: DashboardProps) {
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100">
      {/* Header */}
      <header className="bg-white border-b border-slate-200 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center">
              <h1 className="text-2xl font-bold bg-gradient-to-r from-blue-600 to-blue-800 bg-clip-text text-transparent">
                PingPad
              </h1>
            </div>
            <div className="flex items-center gap-4">
              <span className="text-sm text-slate-600">
                Welcome, <span className="font-semibold text-slate-900">{user.name}</span>
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={onLogout}
                className="gap-2"
              >
                <LogOut className="h-4 w-4" />
                Logout
              </Button>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <ApiEndpoints />
      </main>
    </div>
  );
}
