package org.atomicworkshop.jammachine.sequencing;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class Pattern
{
    public static final byte TRACK_COLOUR = -1;

    private boolean[][] patternData = new boolean[16][25];
    //cable colour for a single note, TRACK_COLOUR uses the sequencer's
    private final byte[][] colours = new byte[16][25];

    public Pattern()
    {
        for (final byte[] row : colours) Arrays.fill(row, TRACK_COLOUR);
    }

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
        if (!checked) colours[interval][pitch] = TRACK_COLOUR;
    }

    public int getColour(int interval, int pitch)
    {
        return colours[interval % 16][pitch % 25];
    }

    public void setColour(int interval, int pitch, int colour)
    {
        colours[interval % 16][pitch % 25] = (byte) colour;
    }

    //interval, pitch, colour for each note with its own colour
    public byte[] writeColours()
    {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int interval = 0; interval < 16; interval++) {
            for (int pitch = 0; pitch < 25; pitch++) {
                if (colours[interval][pitch] == TRACK_COLOUR) continue;
                out.write(interval);
                out.write(pitch);
                out.write(colours[interval][pitch]);
            }
        }
        return out.toByteArray();
    }

    public void readColours(byte[] data)
    {
        for (final byte[] row : colours) Arrays.fill(row, TRACK_COLOUR);
        for (int i = 0; i + 2 < data.length; i += 3) {
            final int interval = data[i], pitch = data[i + 1];
            if (interval < 0 || interval >= 16 || pitch < 0 || pitch >= 25) continue;
            colours[interval][pitch] = data[i + 2];
        }
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
