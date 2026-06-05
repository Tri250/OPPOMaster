import { motion } from "framer-motion";
import { useEffect, useState } from "react";

interface Props {
  end: number;
  suffix?: string;
  prefix?: string;
  duration?: number;
  decimals?: number;
}

export default function AnimatedCounter({ end, suffix = "", prefix = "", duration = 1.6, decimals = 0 }: Props) {
  const [value, setValue] = useState(0);

  useEffect(() => {
    let start: number | null = null;
    const startVal = 0;
    const step = (timestamp: number) => {
      if (!start) start = timestamp;
      const progress = Math.min((timestamp - start) / (duration * 1000), 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      setValue(startVal + (end - startVal) * eased);
      if (progress < 1) requestAnimationFrame(step);
    };
    requestAnimationFrame(step);
  }, [end, duration]);

  return (
    <motion.span
      initial={{ opacity: 0 }}
      whileInView={{ opacity: 1 }}
      viewport={{ once: true }}
    >
      {prefix}
      {value.toFixed(decimals)}
      {suffix}
    </motion.span>
  );
}
