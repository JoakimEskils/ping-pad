import '@testing-library/jest-dom'
import { vi, beforeEach } from 'vitest'

// Mock fetch globally
global.fetch = vi.fn() as any

// Mock window.location
Object.defineProperty(window, 'location', {
  value: {
    href: 'http://localhost:3000',
    assign: vi.fn(),
    replace: vi.fn(),
    reload: vi.fn(),
  },
  writable: true,
})

// Clean up after each test
beforeEach(() => {
  vi.clearAllMocks()
})