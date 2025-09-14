package testing

import (
	"context"
	"time"
)

// RateLimiter implements a token bucket rate limiter
type RateLimiter struct {
	tokens chan struct{}
	ticker *time.Ticker
	ctx    context.Context
	cancel context.CancelFunc
}

// NewRateLimiter creates a new rate limiter
func NewRateLimiter(ratePerSecond int) *RateLimiter {
	if ratePerSecond <= 0 {
		ratePerSecond = 100 // default rate
	}

	ctx, cancel := context.WithCancel(context.Background())
	rl := &RateLimiter{
		tokens: make(chan struct{}, ratePerSecond),
		ctx:    ctx,
		cancel: cancel,
	}

	// Fill the bucket initially
	for i := 0; i < ratePerSecond; i++ {
		rl.tokens <- struct{}{}
	}

	// Start refilling tokens
	rl.ticker = time.NewTicker(time.Second / time.Duration(ratePerSecond))
	go rl.refill()

	return rl
}

// Wait blocks until a token is available
func (rl *RateLimiter) Wait() {
	select {
	case <-rl.tokens:
		return
	case <-rl.ctx.Done():
		return
	}
}

// refill continuously adds tokens to the bucket
func (rl *RateLimiter) refill() {
	for {
		select {
		case <-rl.ticker.C:
			select {
			case rl.tokens <- struct{}{}:
			default:
				// Bucket is full, skip
			}
		case <-rl.ctx.Done():
			return
		}
	}
}

// Close stops the rate limiter
func (rl *RateLimiter) Close() {
	rl.cancel()
	rl.ticker.Stop()
}
