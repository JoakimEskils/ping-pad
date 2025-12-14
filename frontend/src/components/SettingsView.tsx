import { useState, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Bell, Key, Shield, Mail, Lock, Plus, Trash2, Eye, EyeOff } from 'lucide-react';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from './ui/dialog';
import type { User } from '../types';
import { getAuthHeaders } from '../utils/auth';

interface ApiKey {
  id: number;
  name: string;
  keyValue: string;
  createdAt: string;
  updatedAt: string;
}

interface SettingsViewProps {
  user: User;
}

export default function SettingsView({ user }: SettingsViewProps) {
  const isTestUser = user.email === 'test-user@pingpad.local' || user.name === 'Test User';
  const [apiKeys, setApiKeys] = useState<ApiKey[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingKey, setEditingKey] = useState<ApiKey | null>(null);
  const [newKeyName, setNewKeyName] = useState('');
  const [newKeyValue, setNewKeyValue] = useState('');
  const [showKeyValue, setShowKeyValue] = useState<Record<number, boolean>>({});
  const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';

  // Profile state
  const [userName, setUserName] = useState(user.name);
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [profileError, setProfileError] = useState<string | null>(null);
  const [profileSuccess, setProfileSuccess] = useState<string | null>(null);

  // Password state
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isUpdatingPassword, setIsUpdatingPassword] = useState(false);
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [passwordSuccess, setPasswordSuccess] = useState<string | null>(null);
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  useEffect(() => {
    if (!isTestUser) {
      loadApiKeys();
    }
  }, [isTestUser]);

  const loadApiKeys = async () => {
    try {
      const response = await fetch(`${backendUrl}/api/api-keys`, {
        headers: getAuthHeaders(),
        credentials: 'include'
      });
      
      if (response.ok) {
        const data = await response.json();
        setApiKeys(data);
      }
    } catch (error) {
      console.error('Error loading API keys:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateKey = () => {
    setEditingKey(null);
    setNewKeyName('');
    setNewKeyValue('');
    setIsModalOpen(true);
  };

  const handleEditKey = (key: ApiKey) => {
    setEditingKey(key);
    setNewKeyName(key.name);
    setNewKeyValue(key.keyValue);
    setIsModalOpen(true);
  };

  const handleSaveKey = async () => {
    if (!newKeyName.trim() || !newKeyValue.trim()) {
      alert('Please fill in both name and value');
      return;
    }

    try {
      const url = editingKey 
        ? `${backendUrl}/api/api-keys/${editingKey.id}`
        : `${backendUrl}/api/api-keys`;
      const method = editingKey ? 'PUT' : 'POST';

      const response = await fetch(url, {
        method,
        headers: {
          ...getAuthHeaders(),
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify({
          name: newKeyName.trim(),
          keyValue: newKeyValue.trim()
        })
      });

      if (response.ok) {
        setIsModalOpen(false);
        loadApiKeys();
      } else {
        const errorText = await response.text();
        alert(`Failed to save API key: ${errorText}`);
      }
    } catch (error) {
      console.error('Error saving API key:', error);
      alert('Failed to save API key');
    }
  };

  const handleDeleteKey = async (keyId: number) => {
    if (!confirm('Are you sure you want to delete this API key?')) {
      return;
    }

    try {
      const response = await fetch(`${backendUrl}/api/api-keys/${keyId}`, {
        method: 'DELETE',
        headers: getAuthHeaders(),
        credentials: 'include'
      });

      if (response.ok) {
        loadApiKeys();
      } else {
        alert('Failed to delete API key');
      }
    } catch (error) {
      console.error('Error deleting API key:', error);
      alert('Failed to delete API key');
    }
  };

  const toggleShowKeyValue = (keyId: number) => {
    setShowKeyValue(prev => ({ ...prev, [keyId]: !prev[keyId] }));
  };

  const maskKeyValue = (value: string) => {
    if (value.length <= 8) return '••••••••';
    return value.substring(0, 4) + '••••••••' + value.substring(value.length - 4);
  };

  const handleUpdateProfile = async () => {
    if (!userName.trim()) {
      setProfileError('Name is required');
      return;
    }

    setIsSavingProfile(true);
    setProfileError(null);
    setProfileSuccess(null);

    try {
      const response = await fetch(`${backendUrl}/api/user/me`, {
        method: 'PUT',
        headers: {
          ...getAuthHeaders(),
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify({
          name: userName.trim()
        })
      });

      if (response.ok) {
        setProfileSuccess('Profile updated successfully');
        setTimeout(() => setProfileSuccess(null), 5000);
      } else {
        const errorData = await response.json();
        setProfileError(errorData.error || 'Failed to update profile');
      }
    } catch (error) {
      console.error('Error updating profile:', error);
      setProfileError('Failed to update profile');
    } finally {
      setIsSavingProfile(false);
    }
  };

  const handleUpdatePassword = async () => {
    setPasswordError(null);
    setPasswordSuccess(null);

    if (!currentPassword || !newPassword || !confirmPassword) {
      setPasswordError('All password fields are required');
      return;
    }

    if (newPassword.length < 6) {
      setPasswordError('New password must be at least 6 characters');
      return;
    }

    if (newPassword !== confirmPassword) {
      setPasswordError('New passwords do not match');
      return;
    }

    setIsUpdatingPassword(true);

    try {
      const response = await fetch(`${backendUrl}/api/user/me/password`, {
        method: 'PUT',
        headers: {
          ...getAuthHeaders(),
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify({
          currentPassword,
          newPassword
        })
      });

      if (response.ok) {
        setPasswordSuccess('Password updated successfully');
        setCurrentPassword('');
        setNewPassword('');
        setConfirmPassword('');
        setTimeout(() => setPasswordSuccess(null), 5000);
      } else {
        const errorData = await response.json();
        setPasswordError(errorData.error || 'Failed to update password');
      }
    } catch (error) {
      console.error('Error updating password:', error);
      setPasswordError('Failed to update password');
    } finally {
      setIsUpdatingPassword(false);
    }
  };

  if (isTestUser) {
    return (
      <div className="relative">
        {/* Blurred background content */}
        <div className="blur-sm pointer-events-none space-y-6">
          <div>
            <h1 className="text-3xl font-bold text-slate-900 mb-2">Settings</h1>
            <p className="text-slate-600">Manage your account settings and preferences</p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Profile Settings */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Mail className="h-5 w-5" />
                  Profile Settings
                </CardTitle>
                <CardDescription>Update your personal information</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="name">Full Name</Label>
                  <Input id="name" defaultValue="John Doe" />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="email">Email Address</Label>
                  <Input id="email" type="email" defaultValue="john@example.com" />
                </div>
                <Button>Save Changes</Button>
              </CardContent>
            </Card>

            {/* Notification Settings */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Bell className="h-5 w-5" />
                  Notifications
                </CardTitle>
                <CardDescription>Configure your notification preferences</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="font-medium">Email Notifications</p>
                    <p className="text-sm text-slate-600">Receive email alerts for failed tests</p>
                  </div>
                  <input type="checkbox" defaultChecked className="w-5 h-5" />
                </div>
                <div className="flex items-center justify-between">
                  <div>
                    <p className="font-medium">Real-time Alerts</p>
                    <p className="text-sm text-slate-600">Get instant notifications for errors</p>
                  </div>
                  <input type="checkbox" defaultChecked className="w-5 h-5" />
                </div>
                <Button variant="outline">Update Preferences</Button>
              </CardContent>
            </Card>

            {/* API Keys */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Key className="h-5 w-5" />
                  API Keys
                </CardTitle>
                <CardDescription>Manage your API authentication keys</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="p-4 bg-slate-50 rounded-lg">
                  <p className="text-sm font-medium text-slate-900 mb-1">Active API Key</p>
                  <p className="text-xs font-mono text-slate-600">pk_live_••••••••••••••••••••</p>
                </div>
                <div className="flex gap-2">
                  <Button variant="outline" size="sm">Regenerate</Button>
                  <Button variant="outline" size="sm">Copy</Button>
                </div>
              </CardContent>
            </Card>

            {/* Security */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Shield className="h-5 w-5" />
                  Security
                </CardTitle>
                <CardDescription>Manage your security settings</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="current-password">Current Password</Label>
                  <Input id="current-password" type="password" />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="new-password">New Password</Label>
                  <Input id="new-password" type="password" />
                </div>
                <Button>Update Password</Button>
              </CardContent>
            </Card>
          </div>
        </div>

        {/* Lock overlay */}
        <div className="absolute inset-0 flex items-center justify-center z-10">
          <div className="bg-white/95 backdrop-blur-sm rounded-2xl shadow-2xl p-12 max-w-md w-full mx-4 border-2 border-slate-200">
            <div className="flex flex-col items-center text-center space-y-6">
              <div className="w-20 h-20 rounded-full bg-gradient-to-br from-red-100 to-red-200 flex items-center justify-center">
                <Lock className="h-10 w-10 text-red-600" />
              </div>
              <div className="space-y-2">
                <h2 className="text-2xl font-bold text-slate-900">Access Restricted</h2>
                <p className="text-slate-600">
                  Cannot access this page as test user
                </p>
              </div>
              <p className="text-sm text-slate-500">
                Settings are only available for registered users. Please create an account to access this feature.
              </p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-slate-900 mb-2">Settings</h1>
        <p className="text-slate-600">Manage your account settings and preferences</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Profile Settings */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Mail className="h-5 w-5" />
              Profile Settings
            </CardTitle>
            <CardDescription>Update your personal information</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="name">Full Name</Label>
              <Input 
                id="name" 
                value={userName}
                onChange={(e) => setUserName(e.target.value)}
                placeholder="Enter your name"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="email">Email Address</Label>
              <Input 
                id="email" 
                type="email" 
                value={user.email}
                disabled
                className="bg-slate-50 cursor-not-allowed"
              />
              <p className="text-xs text-slate-500">Email cannot be changed</p>
            </div>
            {profileError && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-md">
                <p className="text-sm text-red-700">{profileError}</p>
              </div>
            )}
            {profileSuccess && (
              <div className="p-3 bg-green-50 border border-green-200 rounded-md">
                <p className="text-sm text-green-700">{profileSuccess}</p>
              </div>
            )}
            <Button 
              onClick={handleUpdateProfile}
              disabled={isSavingProfile || userName.trim() === user.name}
            >
              {isSavingProfile ? 'Saving...' : 'Save Changes'}
            </Button>
          </CardContent>
        </Card>

        {/* Notification Settings */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Bell className="h-5 w-5" />
              Notifications
            </CardTitle>
            <CardDescription>Configure your notification preferences</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="font-medium">Email Notifications</p>
                <p className="text-sm text-slate-600">Receive email alerts for failed tests</p>
              </div>
              <input type="checkbox" defaultChecked className="w-5 h-5" />
            </div>
            <div className="flex items-center justify-between">
              <div>
                <p className="font-medium">Real-time Alerts</p>
                <p className="text-sm text-slate-600">Get instant notifications for errors</p>
              </div>
              <input type="checkbox" defaultChecked className="w-5 h-5" />
            </div>
            <Button variant="outline">Update Preferences</Button>
          </CardContent>
        </Card>

        {/* API Keys */}
        <Card className="lg:col-span-2">
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="flex items-center gap-2">
                  <Key className="h-5 w-5" />
                  API Keys
                </CardTitle>
                <CardDescription>Manage your API authentication keys for easy endpoint configuration</CardDescription>
              </div>
              <Button onClick={handleCreateKey} size="sm" className="gap-2">
                <Plus className="h-4 w-4" />
                Add API Key
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="text-center py-8 text-slate-500">Loading API keys...</div>
            ) : apiKeys.length === 0 ? (
              <div className="text-center py-8">
                <p className="text-slate-500 mb-4">No API keys yet</p>
                <Button onClick={handleCreateKey} variant="outline" size="sm" className="gap-2">
                  <Plus className="h-4 w-4" />
                  Create Your First API Key
                </Button>
              </div>
            ) : (
              <div className="space-y-3">
                {apiKeys.map((key) => (
                  <div key={key.id} className="flex items-center justify-between p-4 bg-slate-50 rounded-lg border border-slate-200">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-3">
                        <div className="flex-1 min-w-0">
                          <p className="font-medium text-slate-900 truncate">{key.name}</p>
                          <div className="flex items-center gap-2 mt-1">
                            <p className="text-xs font-mono text-slate-600">
                              {showKeyValue[key.id] ? key.keyValue : maskKeyValue(key.keyValue)}
                            </p>
                            <button
                              onClick={() => toggleShowKeyValue(key.id)}
                              className="text-slate-400 hover:text-slate-600"
                              title={showKeyValue[key.id] ? 'Hide' : 'Show'}
                            >
                              {showKeyValue[key.id] ? (
                                <EyeOff className="h-3 w-3" />
                              ) : (
                                <Eye className="h-3 w-3" />
                              )}
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center gap-2 ml-4">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleEditKey(key)}
                      >
                        Edit
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleDeleteKey(key.id)}
                        className="text-red-600 hover:text-red-700 hover:bg-red-50"
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Security */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Shield className="h-5 w-5" />
              Security
            </CardTitle>
            <CardDescription>Change your password</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="current-password">Current Password</Label>
              <div className="relative">
                <Input 
                  id="current-password" 
                  type={showCurrentPassword ? "text" : "password"}
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  placeholder="Enter current password"
                />
                <button
                  type="button"
                  onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                >
                  {showCurrentPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="new-password">New Password</Label>
              <div className="relative">
                <Input 
                  id="new-password" 
                  type={showNewPassword ? "text" : "password"}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="Enter new password (min 6 characters)"
                />
                <button
                  type="button"
                  onClick={() => setShowNewPassword(!showNewPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                >
                  {showNewPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="confirm-password">Confirm New Password</Label>
              <div className="relative">
                <Input 
                  id="confirm-password" 
                  type={showConfirmPassword ? "text" : "password"}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Confirm new password"
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                >
                  {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>
            {passwordError && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-md">
                <p className="text-sm text-red-700">{passwordError}</p>
              </div>
            )}
            {passwordSuccess && (
              <div className="p-3 bg-green-50 border border-green-200 rounded-md">
                <p className="text-sm text-green-700">{passwordSuccess}</p>
              </div>
            )}
            <Button 
              onClick={handleUpdatePassword}
              disabled={isUpdatingPassword || !currentPassword || !newPassword || !confirmPassword}
            >
              {isUpdatingPassword ? 'Updating...' : 'Update Password'}
            </Button>
          </CardContent>
        </Card>
      </div>

      {/* Create/Edit API Key Dialog */}
      <Dialog open={isModalOpen} onOpenChange={setIsModalOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editingKey ? 'Edit API Key' : 'Create API Key'}</DialogTitle>
            <DialogDescription>
              {editingKey 
                ? 'Update your API key name and value'
                : 'Add a new API key to use when creating endpoints'}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="key-name">Name *</Label>
              <Input
                id="key-name"
                placeholder="e.g., Stripe API Key, GitHub Token"
                value={newKeyName}
                onChange={(e) => setNewKeyName(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="key-value">Value *</Label>
              <Input
                id="key-value"
                type="password"
                placeholder="Enter your API key value"
                value={newKeyValue}
                onChange={(e) => setNewKeyValue(e.target.value)}
              />
            </div>
            <div className="flex justify-end gap-3 pt-4">
              <Button variant="outline" onClick={() => setIsModalOpen(false)}>
                Cancel
              </Button>
              <Button onClick={handleSaveKey}>
                {editingKey ? 'Update' : 'Create'} API Key
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
