#!/bin/bash
ROOT="$(cd "$(dirname "$0")" && pwd)"

# Start backend API
cd "$ROOT/backend" && npm run dev &
BACKEND_PID=$!

# Start frontend dev server on the exposed port
cd "$ROOT/frontend" && npm run dev

trap "kill $BACKEND_PID" EXIT
