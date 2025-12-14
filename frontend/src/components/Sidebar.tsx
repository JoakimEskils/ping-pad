import { LayoutDashboard, Activity, Settings, LogOut, X, Bell } from 'lucide-react';
import { Link, useLocation } from 'react-router-dom';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import { cn } from '../lib/utils';
import { useState, useEffect } from 'react';
import { getAuthHeaders } from '../utils/auth';

interface SidebarProps {
  activeView: string;
  onViewChange: (view: string) => void;
  onLogout: () => void;
  userName: string;
  isMobileOpen?: boolean;
  onMobileToggle?: () => void;
}

export default function Sidebar({
  activeView: _activeView, // Keep for interface compatibility but use location instead
  onViewChange,
  onLogout,
  userName,
  isMobileOpen = false,
  onMobileToggle,
}: SidebarProps) {
  const location = useLocation();
  const [alarmCount, setAlarmCount] = useState(0);
  
  useEffect(() => {
    loadAlarmCount();
    // Refresh alarm count every 10 seconds
    const interval = setInterval(loadAlarmCount, 10000);
    return () => clearInterval(interval);
  }, []);

  const loadAlarmCount = async () => {
    try {
      const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';
      const response = await fetch(`${backendUrl}/api/alarms/count`, {
        headers: getAuthHeaders(),
        credentials: 'include'
      });

      if (response.ok) {
        const data = await response.json();
        setAlarmCount(data.count || 0);
      }
    } catch (error) {
      console.error('Error loading alarm count:', error);
    }
  };
  
  const menuItems: Array<{
    id: string;
    label: string;
    icon: React.ComponentType<{ className?: string }>;
    path: string;
    badge?: number;
  }> = [
    {
      id: 'dashboard',
      label: 'Dashboard',
      icon: LayoutDashboard,
      path: '/dashboard',
    },
    {
      id: 'endpoints',
      label: 'My Endpoints',
      icon: Activity,
      path: '/endpoints',
    },
    {
      id: 'alarms',
      label: 'Alarms',
      icon: Bell,
      path: '/alarms',
      badge: alarmCount > 0 ? alarmCount : undefined,
    },
    {
      id: 'settings',
      label: 'Settings',
      icon: Settings,
      path: '/settings',
    },
  ];

  return (
    <>
      {/* Mobile Overlay */}
      {isMobileOpen && (
        <div
          className="fixed inset-0 bg-black/50 z-40 lg:hidden"
          onClick={onMobileToggle}
        />
      )}

      {/* Sidebar */}
      <aside
        className={cn(
          'fixed left-0 top-0 z-50 h-screen w-64 bg-slate-900 border-r border-slate-800 transition-transform duration-300 ease-in-out',
          'lg:translate-x-0',
          isMobileOpen ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        <div className="flex flex-col h-full">
          {/* Header */}
          <div className="flex items-center justify-between p-6 border-b border-slate-800">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg flex items-center justify-center">
                <Activity className="h-5 w-5 text-white" />
              </div>
              <span className="text-xl font-bold text-white">PingPad</span>
            </div>
            {onMobileToggle && (
              <Button
                variant="ghost"
                size="icon"
                onClick={onMobileToggle}
                className="lg:hidden text-slate-400 hover:text-white hover:bg-slate-800"
              >
                <X className="h-5 w-5" />
              </Button>
            )}
          </div>

          {/* User Info */}
          <div className="p-4 border-b border-slate-800">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-white font-semibold">
                {userName.charAt(0).toUpperCase()}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-white truncate">{userName}</p>
                <p className="text-xs text-slate-400 truncate">Administrator</p>
              </div>
            </div>
          </div>

          {/* Navigation */}
          <nav className="flex-1 p-4 space-y-1 overflow-y-auto">
            {menuItems.map((item) => {
              const Icon = item.icon;
              const isActive = location.pathname.startsWith(item.path);
              return (
                <Link
                  key={item.id}
                  to={item.path}
                  onClick={() => {
                    onViewChange(item.id);
                    onMobileToggle?.();
                  }}
                  className={cn(
                    'w-full flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition-colors',
                    isActive
                      ? 'bg-blue-600 text-white shadow-lg shadow-blue-600/20'
                      : 'text-slate-300 hover:bg-slate-800 hover:text-white'
                  )}
                >
                  <Icon className={cn('h-5 w-5', isActive ? 'text-white' : 'text-slate-400')} />
                  <span className="flex-1">{item.label}</span>
                  {item.badge !== undefined && item.badge > 0 && (
                    <Badge className="bg-red-600 text-white min-w-[20px] flex items-center justify-center">
                      {item.badge > 99 ? '99+' : item.badge}
                    </Badge>
                  )}
                </Link>
              );
            })}
          </nav>

          {/* Footer */}
          <div className="p-4 border-t border-slate-800">
            <Button
              variant="ghost"
              onClick={onLogout}
              className="w-full justify-start gap-3 text-slate-300 hover:text-white hover:bg-slate-800"
            >
              <LogOut className="h-5 w-5" />
              <span>Logout</span>
            </Button>
          </div>
        </div>
      </aside>
    </>
  );
}
