import { useEffect, useState } from 'react';
import { Routes, Route, useNavigate, useLocation, Navigate } from 'react-router-dom';
import { Menu } from 'lucide-react';
import type { User } from '../types';
import Sidebar from './Sidebar';
import DashboardView from './DashboardView';
import ApiEndpoints from './ApiEndpoints';
import SettingsView from './SettingsView';
import { Button } from './ui/button';

interface DashboardProps {
  user: User;
  onLogout: () => void;
}

export default function Dashboard({ user, onLogout }: DashboardProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const [isMobileOpen, setIsMobileOpen] = useState(false);

  // Get active view from path
  const getActiveView = () => {
    if (location.pathname.startsWith('/settings')) return 'settings';
    if (location.pathname.startsWith('/endpoints')) return 'endpoints';
    return 'dashboard';
  };

  const handleViewChange = (view: string) => {
    switch (view) {
      case 'dashboard':
        navigate('/dashboard');
        break;
      case 'endpoints':
        navigate('/endpoints');
        break;
      case 'settings':
        navigate('/settings');
        break;
    }
  };

  // Redirect root to dashboard
  useEffect(() => {
    if (location.pathname === '/') {
      navigate('/dashboard', { replace: true });
    }
  }, [location.pathname, navigate]);

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100">
      {/* Sidebar */}
      <Sidebar
        activeView={getActiveView()}
        onViewChange={handleViewChange}
        onLogout={onLogout}
        userName={user.name}
        isMobileOpen={isMobileOpen}
        onMobileToggle={() => setIsMobileOpen(!isMobileOpen)}
      />

      {/* Main Content Area */}
      <div className="lg:pl-64">
        {/* Mobile Header */}
        <header className="lg:hidden bg-white border-b border-slate-200 shadow-sm sticky top-0 z-30">
          <div className="flex items-center justify-between px-4 h-16">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg flex items-center justify-center">
                <span className="text-white font-bold text-sm">P</span>
              </div>
              <span className="text-xl font-bold text-slate-900">PingPad</span>
            </div>
            <Button
              variant="ghost"
              size="icon"
              onClick={() => setIsMobileOpen(!isMobileOpen)}
              className="lg:hidden"
            >
              <Menu className="h-6 w-6" />
            </Button>
          </div>
        </header>

        {/* Content */}
        <main className="p-4 sm:p-6 lg:p-8">
          <Routes>
            <Route path="/dashboard" element={<DashboardView />} />
            <Route path="/endpoints" element={<ApiEndpoints />} />
            <Route path="/settings" element={<SettingsView user={user} />} />
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </main>
      </div>
    </div>
  );
}
