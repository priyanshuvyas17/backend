/**
 * Global error handler - catch unhandled promise rejections.
 * Prevents app reload loops and silent crashes.
 */

export function setupGlobalErrorHandler() {
  const g = global as any;
  if (g.ErrorUtils?.getGlobalHandler) {
    const original = g.ErrorUtils.getGlobalHandler();
    let handling = false;
    g.ErrorUtils.setGlobalHandler((error: Error, isFatal?: boolean) => {
      if (!handling) {
        handling = true;
        console.error('[GlobalError]', isFatal ? 'FATAL' : 'NON-FATAL', error?.message);
        original(error, isFatal);
        setTimeout(() => { handling = false; }, 500);
      } else {
        original(error, isFatal);
      }
    });
  }

  if (typeof g.HermesInternal?.enablePromiseRejectionTracker === 'function') {
    g.HermesInternal.enablePromiseRejectionTracker({
      allRejections: true,
      onUnhandled: (_id: string, rejection: unknown) => {
        console.error('[UnhandledRejection]', rejection);
      },
      onHandled: () => {},
    });
  }
}
