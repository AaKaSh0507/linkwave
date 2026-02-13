type LogLevel = "debug" | "info" | "warn" | "error";

interface LogEntry {
    level: LogLevel;
    message: string;
    timestamp: string;
    context?: string;
    [key: string]: unknown;
}

const IS_PRODUCTION = process.env.NODE_ENV === "production";

function formatLog(entry: LogEntry): string {
    if (IS_PRODUCTION) {
        return JSON.stringify(entry);
    }
    const { level, message, timestamp, context, ...meta } = entry;
    const prefix = context ? `[${context}]` : "";
    const metaStr = Object.keys(meta).length > 0 ? ` ${JSON.stringify(meta)}` : "";
    return `${timestamp} ${level.toUpperCase().padEnd(5)} ${prefix} ${message}${metaStr}`;
}

function createEntry(
    level: LogLevel,
    message: string,
    context?: string,
    meta?: Record<string, unknown>
): LogEntry {
    return {
        level,
        message,
        timestamp: new Date().toISOString(),
        ...(context && { context }),
        ...meta,
    };
}

export const logger = {
    debug(message: string, context?: string, meta?: Record<string, unknown>): void {
        if (IS_PRODUCTION) return;
        const entry = createEntry("debug", message, context, meta);
        console.debug(formatLog(entry)); // eslint-disable-line no-console -- pre-commit:ignore
    },

    info(message: string, context?: string, meta?: Record<string, unknown>): void {
        const entry = createEntry("info", message, context, meta);
        // eslint-disable-next-line no-console
        console.info(formatLog(entry));
    },

    warn(message: string, context?: string, meta?: Record<string, unknown>): void {
        const entry = createEntry("warn", message, context, meta);
        // eslint-disable-next-line no-console
        console.warn(formatLog(entry));
    },

    error(
        message: string,
        context?: string,
        meta?: Record<string, unknown>
    ): void {
        const entry = createEntry("error", message, context, meta);
        // eslint-disable-next-line no-console
        console.error(formatLog(entry));
    },
};
