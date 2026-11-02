import { useEffect, useState } from 'react';

export default function Countdown({ deadline }: { deadline: number }) {
  const [now, setNow] = useState(Date.now());

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 250);
    return () => window.clearInterval(timer);
  }, []);

  if (!deadline) return null;
  const seconds = Math.max(0, Math.ceil((deadline - now) / 1000));
  return <span className={seconds <= 5 ? 'timer urgent' : 'timer'}>{seconds}s</span>;
}
