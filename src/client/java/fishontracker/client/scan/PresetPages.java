package fishontracker.client.scan;

/**
 * Math for grouping preset numbers into pages of {@value #PRESETS_PER_PAGE}.
 * Kept free of Minecraft dependencies so it can be unit tested.
 */
public final class PresetPages {

    public static final int PRESETS_PER_PAGE = 5;

    private PresetPages() {
    }

    /** Returns the 1-based page group a preset number belongs to (1 for presets 1-5, etc.). */
    public static int pageGroup(int presetNumber) {
        return ((presetNumber - 1) / PRESETS_PER_PAGE) + 1;
    }

    /** Returns the inclusive label of the page, e.g. {@code "6-10"} for preset 7. */
    public static String pageRange(int presetNumber) {
        int group = pageGroup(presetNumber);
        int start = (group - 1) * PRESETS_PER_PAGE + 1;
        int end = group * PRESETS_PER_PAGE;
        return start + "-" + end;
    }
}
