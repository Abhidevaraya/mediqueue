import React from 'react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { Card, CardContent } from '@/components/ui/Card';
import { FileQuestion, Home, ArrowLeft } from 'lucide-react';

export const NotFoundPage: React.FC = () => {
  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-brand-bg text-text-primary">
      <Card className="max-w-md w-full shadow-lg border-surface-border text-center">
        <CardContent className="p-8 space-y-6">
          <div className="w-16 h-16 mx-auto rounded-2xl bg-brand-surface border border-brand-lavender/30 flex items-center justify-center text-brand-lavender">
            <FileQuestion className="w-8 h-8" />
          </div>

          <div className="space-y-2">
            <div className="text-4xl font-extrabold text-brand-lavender font-heading">404</div>
            <h1 className="text-2xl font-bold text-text-primary">Page Not Found</h1>
            <p className="text-sm text-text-secondary leading-relaxed">
              The page or resource you requested could not be located. It may have moved or the link
              might be incorrect.
            </p>
          </div>

          <div className="pt-2 flex flex-col sm:flex-row gap-3">
            <Button
              variant="secondary"
              size="md"
              className="flex-1"
              onClick={() => window.history.back()}
              leftIcon={<ArrowLeft className="w-4 h-4" />}
            >
              Go Back
            </Button>
            <Link to="/" className="flex-1">
              <Button variant="primary" size="md" className="w-full" leftIcon={<Home className="w-4 h-4" />}>
                Return Home
              </Button>
            </Link>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
