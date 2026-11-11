import React, { useState, useRef, useCallback } from 'react';

interface BeforeAfterSliderProps {
  originalUrl: string;
  enhancedUrl: string;
}

export const BeforeAfterSlider: React.FC<BeforeAfterSliderProps> = ({
  originalUrl,
  enhancedUrl,
}) => {
  const [splitPos, setSplitPos] = useState<number>(50); // percentage 0 - 100
  const containerRef = useRef<HTMLDivElement>(null);
  const isDragging = useRef<boolean>(false);

  const handleMove = useCallback((clientX: number) => {
    if (!containerRef.current) return;
    const rect = containerRef.current.getBoundingClientRect();
    const x = clientX - rect.left;
    const pct = Math.max(2, Math.min(98, (x / rect.width) * 100));
    setSplitPos(pct);
  }, []);

  const onTouchMove = (e: React.TouchEvent) => {
    if (e.touches.length > 0) handleMove(e.touches[0].clientX);
  };

  const onMouseDown = () => {
    isDragging.current = true;
  };

  const onMouseMove = (e: React.MouseEvent) => {
    if (isDragging.current) handleMove(e.clientX);
  };

  const onMouseUp = () => {
    isDragging.current = false;
  };

  return (
    <div
      ref={containerRef}
      className="relative w-full h-full overflow-hidden select-none touch-none cursor-ew-resize bg-black"
      onTouchMove={onTouchMove}
      onMouseDown={onMouseDown}
      onMouseMove={onMouseMove}
      onMouseUp={onMouseUp}
      onMouseLeave={onMouseUp}
      style={{
        position: 'relative',
        width: '100%',
        height: '100%',
        overflow: 'hidden',
        userSelect: 'none',
        touchAction: 'none',
        cursor: 'ew-resize',
        backgroundColor: '#080808',
      }}
    >
      {/* Enhanced Image (Background layer) */}
      <img
        src={enhancedUrl}
        alt="Enhanced"
        style={{
          position: 'absolute',
          top: 0,
          left: 0,
          width: '100%',
          height: '100%',
          objectFit: 'contain',
          pointerEvents: 'none',
        }}
      />

      {/* Original Image (Clipped layer on left) */}
      <div
        style={{
          position: 'absolute',
          top: 0,
          left: 0,
          width: `${splitPos}%`,
          height: '100%',
          overflow: 'hidden',
          pointerEvents: 'none',
        }}
      >
        <img
          src={originalUrl}
          alt="Original"
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            width: containerRef.current ? `${containerRef.current.clientWidth}px` : '100vw',
            height: '100%',
            objectFit: 'contain',
            pointerEvents: 'none',
          }}
        />
      </div>

      {/* Divider line */}
      <div
        style={{
          position: 'absolute',
          top: 0,
          bottom: 0,
          left: `${splitPos}%`,
          width: '2px',
          backgroundColor: '#ffffff',
          transform: 'translateX(-50%)',
          boxShadow: '0 0 8px rgba(0,0,0,0.8)',
          pointerEvents: 'none',
        }}
      >
        {/* Circular grip handle */}
        <div
          style={{
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            width: '34px',
            height: '34px',
            borderRadius: '50%',
            backgroundColor: '#ffffff',
            boxShadow: '0 2px 10px rgba(0,0,0,0.6)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <div
            style={{
              width: '10px',
              height: '10px',
              borderRadius: '50%',
              backgroundColor: '#ffd60a',
            }}
          />
        </div>
      </div>

      {/* Labels */}
      <div
        style={{
          position: 'absolute',
          top: '16px',
          left: '16px',
          backgroundColor: 'rgba(0,0,0,0.65)',
          padding: '4px 10px',
          borderRadius: '6px',
          fontSize: '11px',
          fontWeight: 'bold',
          color: '#ffffff',
          letterSpacing: '1px',
          pointerEvents: 'none',
        }}
      >
        ORIGINAL
      </div>

      <div
        style={{
          position: 'absolute',
          top: '16px',
          right: '16px',
          backgroundColor: 'rgba(0,0,0,0.65)',
          padding: '4px 10px',
          borderRadius: '6px',
          fontSize: '11px',
          fontWeight: 'bold',
          color: '#ffd60a',
          letterSpacing: '1px',
          pointerEvents: 'none',
        }}
      >
        AI PRO ENHANCED
      </div>
    </div>
  );
};
