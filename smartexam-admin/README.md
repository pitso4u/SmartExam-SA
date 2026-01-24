# SmartExam Admin Portal

The Management Portal for the SmartExam SA ecosystem. Built with Next.js, it allows administrators to create, validate, and publish CAPS-compliant question packs to the marketplace.

## Features
- **Real-time Stat Dashboard**: Monitor live questions, active packs, and revenue.
- **Marketplace Inventory**: Full CRUD for Question Packs.
- **CAPS Validation**: Enforces pedagogical alignment (cognitive levels, weightings) at the source.
- **Firebase Admin Integration**: Secure server-side access to Firestore/Auth.

## Tech Stack
- **Framework**: Next.js 14
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **Database**: Firebase Admin SDK (Firestore)
- **Icons**: Lucide React

## Local Development

### Prerequisites
- Node.js 18+
- NPM or PNPM

### Setup
1.  Navigate to this directory: `cd smartexam-admin`
2.  Install dependencies: `npm install`
3.  Configure Environment:
    - Copy `.env.example` to `.env.local`
    - Provide your Firebase Service Account credentials.
4.  Run development server: `npm run dev`

### SWC vs Babel
The project is explicitly configured with a `.babelrc` file to bypass SWC binary incompatibility issues on some Windows environments (e.g. "not a valid Win32 application" error). Do not remove this file unless you are migrating to a non-Windows production environment.

## Deployment
1.  Set environment variables in your deployment platform (Vercel, Netlify, etc.).
2.  Run `npm run build`.
3.  The portal is ready to serve at port 3000.
