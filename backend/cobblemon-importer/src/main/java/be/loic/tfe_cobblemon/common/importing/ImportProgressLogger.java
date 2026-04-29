package be.loic.tfe_cobblemon.common.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class ImportProgressLogger {

    private static final Logger log = LoggerFactory.getLogger(ImportProgressLogger.class);

    public ProgressSession start(String label, int total) {
        return new ProgressSession(label, total);
    }

    public static final class ProgressSession {
        private final String label;
        private final int total;
        private final Instant startedAt;
        private int current;
        private int lastLoggedPercent;

        private ProgressSession(String label, int total) {
            this.label = label;
            this.total = Math.max(total, 0);
            this.startedAt = Instant.now();
            this.current = 0;
            this.lastLoggedPercent = -1;

            log.info("[IMPORT] [{}] démarrage | total={}", label, this.total);
        }

        public void advance() {
            advance(null, 1);
        }

        public void advance(String detail) {
            advance(detail, 1);
        }

        public void advance(String detail, int increment) {
            this.current = Math.min(this.current + Math.max(increment, 1), Math.max(this.total, 1));

            if (shouldLog()) {
                int percent = this.total == 0 ? 100 : (int) ((this.current * 100.0) / this.total);
                String elapsed = format(Duration.between(this.startedAt, Instant.now()));

                if (detail == null || detail.isBlank()) {
                    log.info("[IMPORT] [{}] {}/{} ({}%) | elapsed={}",
                            this.label, this.current, this.total, percent, elapsed);
                } else {
                    log.info("[IMPORT] [{}] {}/{} ({}%) | {} | elapsed={}",
                            this.label, this.current, this.total, percent, detail, elapsed);
                }

                this.lastLoggedPercent = percent;
            }
        }

        public void info(String message, Object... args) {
            log.info("[IMPORT] [{}] " + message, prependLabelArgs(args));
        }

        public void success() {
            String elapsed = format(Duration.between(this.startedAt, Instant.now()));
            log.info("[IMPORT] [{}] terminé | total={} | durée={}", this.label, this.total, elapsed);
        }

        public void success(String summary) {
            String elapsed = format(Duration.between(this.startedAt, Instant.now()));
            log.info("[IMPORT] [{}] terminé | {} | durée={}", this.label, summary, elapsed);
        }

        private boolean shouldLog() {
            if (this.total <= 0) {
                return true;
            }

            int percent = (int) ((this.current * 100.0) / this.total);

            return this.current == 1
                    || this.current == this.total
                    || percent >= this.lastLoggedPercent + 5;
        }

        private Object[] prependLabelArgs(Object[] args) {
            Object[] newArgs = new Object[args.length + 1];
            newArgs[0] = this.label;
            System.arraycopy(args, 0, newArgs, 1, args.length);
            return newArgs;
        }

        private static String format(Duration duration) {
            long seconds = duration.getSeconds();
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            long secs = seconds % 60;

            return String.format("%02dh %02dm %02ds", hours, minutes, secs);
        }
    }
}