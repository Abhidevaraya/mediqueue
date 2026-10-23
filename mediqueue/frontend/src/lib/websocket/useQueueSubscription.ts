import { useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { ConnectionState, queueStompManager } from '@/lib/websocket/stompClient';
import { QueueRealTimeMessage } from '@/types/queue';

interface UseQueueSubscriptionOptions {
  doctorId?: number;
  isPatient?: boolean;
  onEvent?: (event: QueueRealTimeMessage) => void;
}

export const useQueueSubscription = (options: UseQueueSubscriptionOptions) => {
  const queryClient = useQueryClient();
  const [connectionState, setConnectionState] = useState<ConnectionState>(
    queueStompManager.getConnectionState()
  );
  const [lastEvent, setLastEvent] = useState<QueueRealTimeMessage | null>(null);

  useEffect(() => {
    const unsubState = queueStompManager.onConnectionStateChange((state) => {
      setConnectionState(state);
    });

    const handleEvent = (event: QueueRealTimeMessage) => {
      setLastEvent(event);
      if (options.onEvent) {
        options.onEvent(event);
      }

      // Automatically invalidate relevant queries to fetch authoritative REST snapshots
      if (options.doctorId) {
        queryClient.invalidateQueries({ queryKey: ['queue', options.doctorId] });
      } else {
        queryClient.invalidateQueries({ queryKey: ['queue'] });
      }
      queryClient.invalidateQueries({ queryKey: ['appointments'] });
    };

    let sub: any = null;

    if (options.doctorId) {
      sub = queueStompManager.subscribeToDoctorQueue(options.doctorId, handleEvent);
    } else if (options.isPatient) {
      sub = queueStompManager.subscribeToPatientQueue(handleEvent);
    }

    return () => {
      unsubState();
      if (sub && sub.unsubscribe) {
        sub.unsubscribe();
      }
    };
  }, [options.doctorId, options.isPatient, queryClient]);

  return {
    connectionState,
    lastEvent,
    isConnected: connectionState === 'CONNECTED',
    isConnecting: connectionState === 'CONNECTING',
  };
};
