package at.helpch.papi.expansion;

import org.jetbrains.annotations.NotNull;

public class ExpansionConfig {
    private Formatting formatting;

    public ExpansionConfig(@NotNull final Formatting formatting) {
        this.formatting = formatting;
    }

    @NotNull
    public Formatting formatting() {
        return formatting;
    }

    public static class Formatting {
        private String thousands;
        private String millions;
        private String billions;
        private String trillions;
        private String quadrillions;

        public Formatting(@NotNull final String thousands, @NotNull final String millions,
                          @NotNull final String billions, @NotNull final String trillions,
                          @NotNull final String quadrillions) {
            this.thousands = thousands;
            this.millions = millions;
            this.billions = billions;
            this.trillions = trillions;
            this.quadrillions = quadrillions;
        }

        @NotNull
        public String thousands() {
            return thousands;
        }

        @NotNull
        public String millions() {
            return millions;
        }

        @NotNull
        public String billions() {
            return billions;
        }

        @NotNull
        public String trillions() {
            return trillions;
        }

        @NotNull
        public String quadrilions() {
            return quadrillions;
        }
    }
}
