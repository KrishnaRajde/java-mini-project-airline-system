import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// host: true lets the phone open this website using the laptop's Wi-Fi IP
export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: 5173
  }
})
