import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { vi, describe, it, beforeEach, expect } from 'vitest'
import App from './App'

describe('App', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders login form when no user is logged in', () => {
    render(<App />)
    
    expect(screen.getByText('PingPad')).toBeInTheDocument()
    expect(screen.getByText('Sign In')).toBeInTheDocument()
    expect(screen.getByText('Login with GitHub')).toBeInTheDocument()
  })

  it('renders GitHub login button', () => {
    render(<App />)
    
    const githubButton = screen.getByText('Login with GitHub')
    expect(githubButton).toBeInTheDocument()
  })

  it('renders login form fields', () => {
    render(<App />)
    
    expect(screen.getByPlaceholderText('Enter your email')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('Enter your password')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument()
  })

  it('handles email input correctly', async () => {
    const user = userEvent.setup()
    render(<App />)
    
    const emailInput = screen.getByPlaceholderText('Enter your email')
    await user.type(emailInput, 'test@example.com')
    
    expect(emailInput).toHaveValue('test@example.com')
  })

  it('handles password input correctly', async () => {
    const user = userEvent.setup()
    render(<App />)
    
    const passwordInput = screen.getByPlaceholderText('Enter your password')
    await user.type(passwordInput, 'password123')
    
    expect(passwordInput).toHaveValue('password123')
  })

  it('shows loading state during login', async () => {
    const user = userEvent.setup()
    render(<App />)
    
    const emailInput = screen.getByPlaceholderText('Enter your email')
    const passwordInput = screen.getByPlaceholderText('Enter your password')
    const submitButton = screen.getByRole('button', { name: /sign in/i })
    
    // Fill in form
    await user.type(emailInput, 'test@example.com')
    await user.type(passwordInput, 'password123')
    
    // Submit form
    await user.click(submitButton)
    
    // Check loading state
    expect(screen.getByText('Signing in...')).toBeInTheDocument()
  })

  it('redirects to GitHub OAuth when GitHub button is clicked', () => {
    const mockAssign = vi.fn()
    Object.defineProperty(window, 'location', {
      value: { href: '', assign: mockAssign },
      writable: true,
    })

    render(<App />)
    
    const githubButton = screen.getByText('Login with GitHub')
    fireEvent.click(githubButton)
    
    expect(mockAssign).toHaveBeenCalledWith('http://localhost:8080/oauth2/authorization/github')
  })
})

describe('App - Authenticated State', () => {
  it('renders dashboard when user is logged in', async () => {
    render(<App />)
    
    // Simulate successful login by filling form and submitting
    const user = userEvent.setup()
    const emailInput = screen.getByPlaceholderText('Enter your email')
    const passwordInput = screen.getByPlaceholderText('Enter your password')
    const submitButton = screen.getByRole('button', { name: /sign in/i })
    
    await user.type(emailInput, 'test@example.com')
    await user.type(passwordInput, 'password123')
    await user.click(submitButton)
    
    // Wait for successful login (mock login takes 1 second)
    await waitFor(
      () => {
        expect(screen.getByText('Welcome, test')).toBeInTheDocument()
      },
      { timeout: 2000 }
    )
    
    expect(screen.getByText('API Endpoints')).toBeInTheDocument()
    expect(screen.getByText('Webhooks')).toBeInTheDocument()
  })

  it('allows user to logout', async () => {
    render(<App />)
    
    // Login first
    const user = userEvent.setup()
    const emailInput = screen.getByPlaceholderText('Enter your email')
    const passwordInput = screen.getByPlaceholderText('Enter your password')
    const submitButton = screen.getByRole('button', { name: /sign in/i })
    
    await user.type(emailInput, 'test@example.com')
    await user.type(passwordInput, 'password123')
    await user.click(submitButton)
    
    // Wait for login
    await waitFor(() => {
      expect(screen.getByText('Welcome, test')).toBeInTheDocument()
    })
    
    // Logout
    const logoutButton = screen.getByText('Logout')
    await user.click(logoutButton)
    
    // Should be back to login form
    expect(screen.getByText('Sign In')).toBeInTheDocument()
  })


  it('shows "Create Endpoint" form', async () => {
    render(<App />)
    
    // Login first
    const user = userEvent.setup()
    const emailInput = screen.getByPlaceholderText('Enter your email')
    const passwordInput = screen.getByPlaceholderText('Enter your password')
    const submitButton = screen.getByRole('button', { name: /sign in/i })
    
    await user.type(emailInput, 'test@example.com')
    await user.type(passwordInput, 'password123')
    await user.click(submitButton)
    
    // Wait for login
    await waitFor(() => {
      expect(screen.getByText('Welcome, test')).toBeInTheDocument()
    })
    
    // Create Endpoint
    const createButton = screen.getByText('Create Endpoint')
    await user.click(createButton)

    expect(screen.getByText('GET')).toBeInTheDocument()

  })
  
})
  
