import { useEffect, useState } from "react";
import {
  Download,
  ExternalLink,
  Minus,
  Plus,
  RotateCcw,
  RotateCw,
  X,
} from "lucide-react";

type DocumentViewerProps = {
  open: boolean;
  url: string;
  contentType: string;
  title: string;
  filename?: string;
  onClose: () => void;
};

const MIN_ZOOM = 50;
const MAX_ZOOM = 400;
const ZOOM_STEP = 25;

export function DocumentViewer({
  open,
  url,
  contentType,
  title,
  filename = "document",
  onClose,
}: DocumentViewerProps) {
  const [zoom, setZoom] = useState(100);
  const [rotation, setRotation] = useState(0);
  const [fitToScreen, setFitToScreen] = useState(true);
  const [naturalSize, setNaturalSize] = useState({ width: 0, height: 0 });
  const isPdf = contentType.toLowerCase().includes("pdf");
  const downloadFilename = /\.[a-z0-9]+$/i.test(filename)
    ? filename
    : `${filename}.${isPdf ? "pdf" : contentType.includes("png") ? "png" : "jpg"}`;

  useEffect(() => {
    if (!open) return;
    setZoom(100);
    setRotation(0);
    setFitToScreen(true);
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
      if (!isPdf && (event.key === "+" || event.key === "=")) {
        setFitToScreen(false);
        setZoom((value) => Math.min(MAX_ZOOM, value + ZOOM_STEP));
      }
      if (!isPdf && event.key === "-") {
        setFitToScreen(false);
        setZoom((value) => Math.max(MIN_ZOOM, value - ZOOM_STEP));
      }
      if (!isPdf && event.key === "0") {
        setZoom(100);
        setRotation(0);
        setFitToScreen(true);
      }
    };
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("keydown", onKeyDown);
      document.body.style.overflow = previousOverflow;
    };
  }, [isPdf, onClose, open]);

  if (!open) return null;

  const changeZoom = (change: number) => {
    setFitToScreen(false);
    setZoom((value) => Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, value + change)));
  };

  return (
    <div
      className="fixed inset-0 z-[100] flex flex-col bg-slate-950"
      role="dialog"
      aria-modal="true"
      aria-label={`${title} preview`}
    >
      <header className="flex min-h-16 flex-wrap items-center justify-between gap-3 border-b border-white/10 bg-slate-900 px-4 py-3 text-white">
        <div className="min-w-0">
          <h2 className="truncate font-bold">{title}</h2>
          <p className="text-xs text-slate-400">
            {isPdf ? "Use the PDF controls to inspect every page" : "Use + / − to inspect details"}
          </p>
        </div>
        <div className="flex flex-wrap items-center justify-end gap-2">
          {!isPdf && (
            <>
              <ViewerButton
                label="Zoom out"
                disabled={zoom <= MIN_ZOOM}
                onClick={() => changeZoom(-ZOOM_STEP)}
              >
                <Minus className="h-4 w-4" />
              </ViewerButton>
              <span
                className="min-w-16 rounded-lg bg-white/10 px-3 py-2 text-center text-sm font-bold"
                aria-live="polite"
              >
                {fitToScreen ? "Fit" : `${zoom}%`}
              </span>
              <ViewerButton
                label="Zoom in"
                disabled={zoom >= MAX_ZOOM}
                onClick={() => changeZoom(ZOOM_STEP)}
              >
                <Plus className="h-4 w-4" />
              </ViewerButton>
              <ViewerButton label="Rotate left" onClick={() => setRotation((value) => value - 90)}>
                <RotateCcw className="h-4 w-4" />
              </ViewerButton>
              <ViewerButton label="Rotate right" onClick={() => setRotation((value) => value + 90)}>
                <RotateCw className="h-4 w-4" />
              </ViewerButton>
              <ViewerButton
                label="Fit to screen"
                onClick={() => {
                  setZoom(100);
                  setRotation(0);
                  setFitToScreen(true);
                }}
              >
                Fit
              </ViewerButton>
              <ViewerButton
                label="View at original size"
                onClick={() => {
                  setZoom(100);
                  setRotation(0);
                  setFitToScreen(false);
                }}
              >
                1:1
              </ViewerButton>
            </>
          )}
          <a
            href={url}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex h-10 items-center gap-2 rounded-lg bg-white/10 px-3 text-sm font-semibold hover:bg-white/20"
          >
            <ExternalLink className="h-4 w-4" /> Open
          </a>
          <a
            href={url}
            download={downloadFilename}
            className="inline-flex h-10 items-center gap-2 rounded-lg bg-white/10 px-3 text-sm font-semibold hover:bg-white/20"
          >
            <Download className="h-4 w-4" /> Download
          </a>
          <button
            type="button"
            onClick={onClose}
            className="inline-flex h-10 items-center gap-2 rounded-lg bg-white px-3 text-sm font-bold text-slate-900 hover:bg-slate-100"
          >
            <X className="h-4 w-4" /> Close
          </button>
        </div>
      </header>

      <main className="min-h-0 flex-1 overflow-auto bg-[radial-gradient(circle_at_center,_#334155_0,_#0f172a_70%)]">
        {isPdf ? (
          <iframe src={url} title={title} className="h-full min-h-[500px] w-full bg-white" />
        ) : (
          <div className="flex min-h-full min-w-full items-center justify-center p-4 sm:p-8">
            <img
              src={url}
              alt={title}
              draggable={false}
              onLoad={(event) =>
                setNaturalSize({
                  width: event.currentTarget.naturalWidth,
                  height: event.currentTarget.naturalHeight,
                })
              }
              className={`select-none shadow-2xl ${
                fitToScreen ? "max-h-[calc(100vh-8rem)] max-w-[calc(100vw-2rem)] object-contain" : "max-w-none"
              }`}
              style={{
                width:
                  !fitToScreen && naturalSize.width
                    ? `${naturalSize.width * (zoom / 100)}px`
                    : undefined,
                height:
                  !fitToScreen && naturalSize.height
                    ? `${naturalSize.height * (zoom / 100)}px`
                    : undefined,
                transform: `rotate(${rotation}deg)`,
                transformOrigin: "center",
              }}
            />
          </div>
        )}
      </main>
    </div>
  );
}

function ViewerButton({
  children,
  label,
  disabled,
  onClick,
}: {
  children: React.ReactNode;
  label: string;
  disabled?: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      title={label}
      aria-label={label}
      disabled={disabled}
      onClick={onClick}
      className="inline-flex h-10 min-w-10 items-center justify-center rounded-lg bg-white/10 px-3 text-sm font-bold hover:bg-white/20 disabled:cursor-not-allowed disabled:opacity-35"
    >
      {children}
    </button>
  );
}
