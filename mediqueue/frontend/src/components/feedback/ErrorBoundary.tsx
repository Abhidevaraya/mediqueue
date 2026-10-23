import { Component, ErrorInfo, ReactNode } from 'react';
import { Button } from '@/components/ui/Button';
import { Card, CardContent } from '@/components/ui/Card';
import { AlertTriangle, RefreshCw } from 'lucide-react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Uncaught component error in MediQueue UI:', error, errorInfo);
  }

  private handleReset = () => {
    this.setState({ hasError: false, error: null });
    window.location.reload();
  };

  public render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center p-4 bg-brand-bg text-text-primary">
          <Card className="max-w-md w-full shadow-lg border-surface-border text-center">
            <CardContent className="p-8 space-y-6">
              <div className="w-16 h-16 mx-auto rounded-2xl bg-status-error-surface border border-status-error/20 flex items-center justify-center text-status-error">
                <AlertTriangle className="w-8 h-8" />
              </div>

              <div className="space-y-2">
                <h1 className="text-2xl font-bold text-text-primary">Something went wrong</h1>
                <p className="text-sm text-text-secondary leading-relaxed">
                  An unexpected UI error occurred while rendering this view. Your session and data are
                  unaffected.
                </p>
              </div>

              <div className="pt-2 flex flex-col gap-3">
                <Button
                  variant="primary"
                  size="md"
                  onClick={this.handleReset}
                  className="w-full"
                  leftIcon={<RefreshCw className="w-4 h-4" />}
                >
                  Reload Application
                </Button>
                <a href="/" className="w-full">
                  <Button variant="secondary" size="md" className="w-full">
                    Return to Homepage
                  </Button>
                </a>
              </div>
            </CardContent>
          </Card>
        </div>
      );
    }

    return this.props.children;
  }
}
