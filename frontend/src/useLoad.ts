import { useCallback, useEffect, useState } from 'react';

/**
 * Loads data when the component mounts and whenever deps change.
 * Results of a stale request (e.g. after switching year quickly) are ignored.
 */
export function useLoad<T>(load: () => Promise<T>, deps: unknown[]) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(true);
  const [version, setVersion] = useState(0);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const stableLoad = useCallback(load, deps);

  useEffect(() => {
    let current = true;
    setLoading(true);
    setError(null);
    stableLoad()
      .then((result) => current && setData(result))
      .catch((e: unknown) => {
        if (current) {
          setData(null);
          setError(e);
        }
      })
      .finally(() => current && setLoading(false));
    return () => {
      current = false;
    };
  }, [stableLoad, version]);

  const reload = useCallback(() => setVersion((v) => v + 1), []);
  return { data, error, loading, reload, setData };
}
