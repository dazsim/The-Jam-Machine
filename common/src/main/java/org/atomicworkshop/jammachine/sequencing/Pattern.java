package org.atomicworkshop.jammachine.sequencing;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class Pattern
{
    private boolean[][] patternData = new boolean[16][25];

    public void setPitchAtInterval(int interval, int pitch)
    {
        setPitchAtInterval(interval, pitch, true);
    }


    public void setPitchAtInterval(int interval, int pitch, boolean checked) {
        if (pitch > 24) {
            pitch %= 25;
        }
        if (interval >= 16) {
            interval %= 16;
        }

        patternData[interval][pitch] = checked;
    }

    public void resetPitchAtInterval(int interval, int pitch) {
        setPitchAtInterval(interval, pitch, false);
    }

    public boolean invertPitchAtInternal(int interval, int pitch)
    {
        while (pitch > 24) {
            pitch -= 25;
        }
        while (interval >= 16) {
            interval -= 16;
        }

        boolean newValue = !patternData[interval][pitch];
        patternData[interval][pitch] = newValue;
        return newValue;
    }

    public Iterable<Byte> getPitchesAtInterval(int interval)
    {
        return () -> new PitchIterator(patternData[interval]);
    }

    public boolean[] getRawPatternData(int interval)
    {
        return patternData[interval];

    }

    public boolean isPitchActiveAtInterval(int interval, int pitch) {
        while (pitch > 24) {
            pitch -= 25;
        }
        while (interval >= 16) {
            interval -= 16;
        }
        return patternData[interval][pitch];
    }

    private static class PitchIterator implements Iterator<Byte>
    {
        private byte currentPitch;
        private final boolean[] enabledPitches;

        public PitchIterator(boolean[] enabledPitches)
        {
            this.enabledPitches = enabledPitches;
            currentPitch = -1;
        }

        @Override
        public boolean hasNext()
        {
            ++currentPitch;
            while (currentPitch < enabledPitches.length) {
                if (enabledPitches[currentPitch]) {
                    return true;
                }
                ++currentPitch;
            }

            return false;
        }

        @Override
        public Byte next()
        {
            if (currentPitch >= enabledPitches.length) {
                throw new NoSuchElementException();
            }
            return currentPitch;
        }
    }
}
