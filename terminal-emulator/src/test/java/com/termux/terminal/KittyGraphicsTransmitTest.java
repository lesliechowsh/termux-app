package com.termux.terminal;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Transmission and placement: a=t/a=T placing images on the buffer, chunked uploads, the
 * cursor movement a placement owes the client, a=p placements and U=1 virtual placements,
 * and the transmission media.
 */
public class KittyGraphicsTransmitTest extends KittyGraphicsTestCase {

    public void testDisplayTransmitPlacesImageAndAnswersOk() {
        enterString("\033[1;2H");
        enterString(apc("a=T,i=7,f=24,s=1,v=1;" + base64(0xAA, 0x55, 0x7E)));
        assertEquals(reply("i=7", "OK"), mOutput.getOutputAndClear());
        assertEquals(1, placements(7).size());
        KittyPlacement placement = placements(7).get(0);
        assertEquals(7, placement.getImageId());
        assertEquals(0, placement.getPlacementId());
        assertEquals(1, placement.getColumn());
        assertEquals(0, placement.getRow());
        assertEquals(1, placement.getWidth());
        assertEquals(1, placement.getHeight());
        // One 1x1 pixel image covers one 13x15 pixel cell, so the cursor advanced one column.
        assertEquals(1, placement.getColumns());
        assertEquals(1, placement.getRows());
        assertCursorAt(0, 2);
    }

    public void testDisplayTransmitWithoutImageIdIsSilent() {
        // The reply only carries what the client addressed; an anonymous display answers
        // nothing, but still places the image.
        enterString(apc("a=T,f=24,s=1,v=1;" + base64(1, 2, 3)));
        assertEquals("", mOutput.getOutputAndClear());
        assertEquals(1, placements(0).size());
        assertCursorAt(0, 1);
    }

    public void testRawPixelCountMustMatchSize() {
        // f=24 asks for s*v*3 bytes: three bytes for a 2x1 image, which needs six, are refused
        // before the cursor moves or anything is placed.
        enterString(apc("a=T,i=7,f=24,s=2,v=1;" + base64(1, 2, 3)));
        assertEquals(reply("i=7", "EINVAL:pixel data does not match s and v"), mOutput.getOutputAndClear());
        assertEquals(0, placements(7).size());
        assertCursorAt(0, 0);
        // f=32 asks for s*v*4 bytes, so the same three bytes are still wrong.
        enterString(apc("a=T,i=7,f=32,s=1,v=1;" + base64(1, 2, 3)));
        assertEquals(reply("i=7", "EINVAL:pixel data does not match s and v"), mOutput.getOutputAndClear());
    }

    public void testRgbaTransmitPlacesImage() {
        enterString(apc("a=T,i=7,f=32,s=1,v=1;" + base64(1, 2, 3, 4)));
        assertEquals(reply("i=7", "OK"), mOutput.getOutputAndClear());
        assertEquals(1, placements(7).size());
    }

    public void testPngHeaderDeterminesPlacementSize() {
        // The harness decodes no pixels, so a correct size here is provably read from the IHDR
        // header: a 2x3 header yields a 2x3 pixel placement without any decode.
        enterString(apc("a=T,i=7,f=100;" + base64(pngHeader(2, 3))));
        assertEquals(reply("i=7", "OK"), mOutput.getOutputAndClear());
        assertEquals(1, placements(7).size());
        KittyPlacement placement = placements(7).get(0);
        assertEquals(2, placement.getWidth());
        assertEquals(3, placement.getHeight());
        assertEquals(1, placement.getColumns()); // ceil(2 / 13)
        assertEquals(1, placement.getRows());    // ceil(3 / 15)
    }

    public void testPngWithGarbageDataIsRefused() {
        enterString(apc("a=T,i=7,f=100;" + base64(1, 2, 3)));
        assertEquals(reply("i=7", "EINVAL:invalid PNG"), mOutput.getOutputAndClear());
        assertEquals(0, placements(7).size());
    }

    public void testChunkedTransmissionAssemblesAcrossCommands() {
        enterString(apc("a=T,i=7,f=24,s=1,v=1,m=1;" + base64(9, 8, 7)));
        assertEquals("", mOutput.getOutputAndClear());
        assertEquals(0, placements(7).size());
        assertCursorAt(0, 0);
        enterString(apc("m=0;"));
        assertEquals(reply("i=7", "OK"), mOutput.getOutputAndClear());
        assertEquals(1, placements(7).size());
        assertCursorAt(0, 1);
    }

    public void testOrphanContinuationChunkIsSilent() {
        // A continuation with no upload in flight is dropped without a word: nothing references
        // an id, so no reply gate passes either.
        enterString(apc("m=0;AAAA"));
        assertEquals("", mOutput.getOutputAndClear());
    }

    public void testPlainTextBetweenChunksDoesNotDisturbTheUpload() {
        enterString(apc("a=T,i=7,f=24,s=1,v=1,m=1;" + base64(1, 2, 3)));
        assertEquals("", mOutput.getOutputAndClear());
        enterString("hi");
        enterString(apc("m=0;"));
        assertEquals(reply("i=7", "OK"), mOutput.getOutputAndClear());
        assertLinesAre("hi   ", "     ", "     ");
        assertEquals(1, placements(7).size());
        assertEquals(2, placements(7).get(0).getColumn());
        assertCursorAt(0, 3);
    }

    public void testCursorAdvancesAcrossImageColumns() {
        // c=2,r=1 displays 26x15 pixels: two cells across, one down; the cursor ends after them.
        enterString(apc("a=T,i=7,f=24,s=1,v=1,c=2,r=1;" + base64(1, 2, 3)));
        KittyPlacement placement = placements(7).get(0);
        assertEquals(2, placement.getColumns());
        assertEquals(1, placement.getRows());
        assertCursorAt(0, 2);
    }

    public void testCursorLinefeedsBelowTallImage() {
        // c=1,r=2 displays 13x30 pixels: the cursor linefeeds once (the first image row shares
        // the cursor's own row) and ends one column past the image's last cell.
        enterString("\033[1;2H");
        enterString(apc("a=T,i=7,f=24,s=1,v=1,c=1,r=2;" + base64(1, 2, 3)));
        assertCursorAt(1, 2);
        KittyPlacement placement = placements(7).get(0);
        assertEquals(1, placement.getColumns());
        assertEquals(2, placement.getRows());
    }

    public void testCursorWrapsAtEndOfRow() {
        // An image placed in the last column linefeeds to the next row's first column.
        enterString("\033[1;5H");
        enterString(apc("a=T,f=24,s=1,v=1;" + base64(1, 2, 3)));
        assertCursorAt(1, 0);
        assertEquals(4, placements(0).get(0).getColumn());
    }

    public void testNoCursorMovementFlagLeavesCursorAlone() {
        // C=1 asks the terminal not to move the cursor; the image still places.
        enterString(apc("a=T,i=7,f=24,s=1,v=1,C=1;" + base64(1, 2, 3)));
        assertCursorAt(0, 0);
        assertEquals(1, placements(7).size());
    }

    public void testCellOffsetIsCountedIntoTheCursorAdvance() {
        // The image may start inside the cursor cell: a 13x15 px image at pixel offset X=12,Y=14
        // spans 25x29 px, so the advance covers 2x2 cells.
        enterString(apc("a=T,i=7,f=24,s=13,v=15,X=12,Y=14;" + base64(new byte[13 * 15 * 3])));
        assertEquals(reply("i=7", "OK"), mOutput.getOutputAndClear());
        assertEquals(1, placements(7).size());
        assertCursorAt(1, 2);
    }

    public void testCellOffsetOutsideTheCellAnswersEinvalWithoutMovingTheCursor() {
        // X is a pixel offset inside the 13 px wide cell: 13 is outside it.
        enterString(apc("a=T,i=7,f=24,s=1,v=1,X=13;" + base64(1, 2, 3)));
        assertEquals(reply("i=7", "EINVAL:cell offset outside cell"), mOutput.getOutputAndClear());
        assertCursorAt(0, 0);
    }

    public void testStoreOnlyTransmitKeepsImageWithoutPlacing() {
        enterString(apc("a=t,i=7,f=24,s=1,v=1;" + base64(1, 2, 3)));
        assertEquals(reply("i=7", "OK"), mOutput.getOutputAndClear());
        assertEquals(0, placements(7).size());
        assertEquals(1, kitty.store().count());
        assertTrue(storedImage(7).completed);
        assertCursorAt(0, 0); // storing never moves the cursor
    }

    public void testStoreOnlyRequiresAnImageId() {
        enterString(apc("a=t,f=24,s=1,v=1;" + base64(1, 2, 3)));
        assertEquals(reply("", "EINVAL:storing an image requires i or I"), mOutput.getOutputAndClear());
        assertEquals(0, kitty.store().count());
    }

    public void testPlacementPlacesStoredImageAtCursor() {
        enterString(apc("a=t,i=7,f=24,s=1,v=1;" + base64(1, 2, 3)));
        mOutput.getOutputAndClear();
        enterString(apc("a=p,i=7"));
        assertEquals(reply("i=7", "OK"), mOutput.getOutputAndClear());
        assertEquals(1, placements(7).size());
        assertEquals(0, placements(7).get(0).getColumn());
        assertCursorAt(0, 1);
    }

    public void testPlacementOfUnknownImageAnswersEnoent() {
        enterString(apc("a=p,i=42"));
        assertEquals(reply("i=42", "ENOENT:image not found"), mOutput.getOutputAndClear());
        assertEquals(0, placements(42).size());
        assertCursorAt(0, 0);
    }

    public void testNumberAddressingAssignsAnIdAndLaterResolvesIt() {
        enterString(apc("a=t,I=3,f=24,s=1,v=1;" + base64(1, 2, 3)));
        assertEquals(reply("i=1,I=3", "OK"), mOutput.getOutputAndClear());
        enterString(apc("a=p,I=3"));
        assertEquals(reply("i=1,I=3", "OK"), mOutput.getOutputAndClear());
        assertEquals(1, placements(1).size());
    }

    public void testRetransmissionUnderSameIdReplacesPlacement() {
        // A display transmission under an id first takes the old placements of that id down.
        enterString(apc("a=T,i=7,f=24,s=1,v=1;" + base64(1, 2, 3)));
        enterString("\033[1;3H");
        enterString(apc("a=T,i=7,f=24,s=1,v=1;" + base64(4, 5, 6)));
        assertEquals(1, placements(7).size());
        assertEquals(2, placements(7).get(0).getColumn());
    }

    public void testPlacementWithIdentifiedIdMovesInsteadOfStacking() {
        enterString(apc("a=t,i=7,f=24,s=1,v=1;" + base64(1, 2, 3)));
        mOutput.getOutputAndClear();
        enterString(apc("a=p,i=7,p=1")); // placement 1 at (0,0), cursor to (0,1)
        enterString(apc("a=p,i=7,p=2")); // placement 2 added at (0,1), cursor to (0,2)
        assertEquals(2, placements(7).size());
        enterString(apc("a=p,i=7,p=2")); // the same placement id moves, it does not stack
        assertEquals(2, placements(7).size());
        int moved = 0;
        for (KittyPlacement placement : placements(7)) {
            if (placement.getPlacementId() == 2) {
                moved++;
                assertEquals(2, placement.getColumn());
            }
        }
        assertEquals(1, moved);
    }

    public void testHugeDisplayClampIsRefusedWithoutHanging() {
        // c/r far past the pixel budget clamp to Integer.MAX_VALUE, where the display size
        // check's product wrapped negative and passed — then the cursor advanced for the
        // clamped height, linefeeding millions of times. The check must refuse, answer, and
        // move nothing.
        enterString(apc("a=T,i=1,f=32,s=1,v=1,c=2147483647,r=2147483647;" + base64(1, 2, 3, 4)));
        assertEquals(reply("i=1", "ENOSPC:display image exceeds session limit"), mOutput.getOutputAndClear());
        assertEquals(0, placements(1).size());
        assertNull(storedImage(1));
        assertCursorAt(0, 0);
        assertLinesAre("     ", "     ", "     ");
        // A cell offset that wraps the clamped width negative must not sneak past either.
        enterString(apc("a=T,i=1,f=32,s=1,v=1,c=2147483647,r=2147483647,X=1;" + base64(1, 2, 3, 4)));
        assertEquals(reply("i=1", "ENOSPC:display image exceeds session limit"), mOutput.getOutputAndClear());
        assertCursorAt(0, 0);
        assertEquals(0, placements(1).size());
    }

    public void testBudgetRefusedReplacementLeavesTheOldPlacementInPlace() {
        // Byte pressure from owning placements: anonymous displays own their pixels.
        // 4096*2047*4 = 33521664 bytes, then 4*1*4 = 16 more; the cap is 33554432.
        enterString(apc("a=T,f=100,c=1,r=1;" + base64(pngHeader(4096, 2047))));
        mOutput.getOutputAndClear();
        enterString(apc("a=T,f=100,c=1,r=1;" + base64(pngHeader(4, 1))));
        mOutput.getOutputAndClear();
        // A stored 1x1 image placed under (i=7, p=9) at column 2.
        enterString(apc("a=t,i=7,f=100;" + base64(pngHeader(1, 1))));
        enterString(apc("a=p,i=7,p=9"));
        mOutput.getOutputAndClear();
        assertEquals(1, placements(7).size());
        // A re-placement of (7, 9) owning 40000 bytes would push the session past the cap:
        // refused, and the placement it replaces must not be taken down with it. The call
        // goes straight to the emulator's placement path, the only harness-reachable way to
        // make a re-placement carry owned bytes: a stored image shares at zero cost.
        KittyGraphicsProtocol.Command replace = KittyGraphicsProtocol.Command.parse("a=p,i=7,p=9");
        boolean placed = mTerminal.placeKittyGraphics(testBitmap(), true, 40000, replace, 7,
            0, 0, new int[] {0, 0, 1, 1}, 13, 15, 0, 0, 13, 15);
        assertFalse(placed);
        assertEquals(1, placements(7).size());
        KittyPlacement kept = placements(7).get(0);
        assertEquals(7, kept.getImageId());
        assertEquals(9, kept.getPlacementId());
        assertEquals(2, kept.getColumn());
        assertEquals(0, kept.getRow());
    }

    public void testUnicodePlacementRequiresAnImageId() {
        // U=1 without i= or I= names no image to place.
        enterString(apc("a=T,U=1,f=24,s=1,v=1;" + base64(1, 2, 3)));
        assertEquals(reply("", "EINVAL:unicode placement requires i or I"), mOutput.getOutputAndClear());
        assertEquals(0, kitty.store().count());
    }

    public void testUnicodePlacementRequiresDisplayAction() {
        // U=1 is a display feature: a store-only command carrying it is refused.
        enterString(apc("a=t,i=5,U=1,f=24,s=1,v=1;" + base64(1, 2, 3)));
        assertEquals(reply("i=5", "EINVAL:U is only valid for display commands"), mOutput.getOutputAndClear());
        assertEquals(0, kitty.store().count());
    }

    public void testUnicodePlacementRegistersPlaceholder() {
        // U=1 stores an invisible placement that U+10EEEE placeholder cells draw through; the
        // command is a display one, but the placement itself is stored, never placed on cells.
        enterString(apc("a=T,i=5,U=1,c=2,r=1,f=24,s=1,v=1;" + base64(1, 2, 3)));
        assertEquals(reply("i=5", "OK"), mOutput.getOutputAndClear());
        assertTrue(mTerminal.hasKittyVirtualPlacement(5, 0));
        KittyImagePlaceholder out = new KittyImagePlaceholder();
        assertTrue(mTerminal.getKittyImagePlaceholder(5, 0, out));
        assertEquals(2, out.columns);
        assertEquals(1, out.rows);
        assertEquals(1, out.sourceWidth);
        assertEquals(1, out.sourceHeight);
        assertTrue(out.generation > 0);
    }

    public void testUnicodePlaceholderCellBackgroundResolvesTheVirtualPlacement() {
        // kitty encodes the image id in a placeholder cell's fg and the virtual placement id in
        // its bg, so a cell resolves through its background. The decode calls below mirror
        // TerminalRenderer's decode call; terminal-view is outside this harness, so this is
        // where that argument contract is pinned.
        enterString(apc("a=T,i=5,U=1,p=3,c=1,r=1,f=24,s=1,v=1;" + base64(1, 2, 3)));
        assertEquals(reply("i=5,p=3", "OK"), mOutput.getOutputAndClear());
        assertTrue(mTerminal.hasKittyVirtualPlacement(5, 3));
        // One U+10EEEE cell carrying row 0, column 0 (Diacritics[0], Diacritics[0]).
        char[] cell = (new String(Character.toChars(KittyUnicodePlaceholder.CODE_POINT))
            + "̅̅").toCharArray();
        enterString("\033[38;2;0;0;5;48;2;0;0;3m"); // fg = image id 5, bg = placement id 3.
        enterString(new String(cell));
        // The placeholder sits at chars 0..3 of the row text: two surrogate chars, then the
        // two diacritics decode() reads.
        char[] line = mTerminal.getScreen().allocateFullLineIfNecessary(
            mTerminal.getScreen().externalToInternalRow(0)).mText;
        KittyUnicodePlaceholder.Cell decoded = KittyUnicodePlaceholder.decode(
            line, 2, 4, TextStyle.decodeForeColor(getStyleAt(0, 0)),
            TextStyle.decodeBackColor(getStyleAt(0, 0)), TextStyle.COLOR_INDEX_FOREGROUND, null);
        assertEquals(5, decoded.imageId);
        assertEquals(3, decoded.placementId);
        KittyImagePlaceholder placement3 = new KittyImagePlaceholder();
        assertTrue(mTerminal.getKittyImagePlaceholder(5, 3, placement3));
        assertEquals(1, placement3.columns); // the p=3 registration asked for c=1
        // A second cell with a different background resolves differently: bg 4 names no virtual
        // placement, so nothing resolves. A default bg decodes to placement id 0, whose lookup
        // answers the image's first virtual placement — the documented fallback.
        enterString("\033[0;38;2;0;0;5;48;2;0;0;4m"); // Reset, then fg = image id 5, bg = 4.
        enterString(new String(cell));
        KittyUnicodePlaceholder.Cell unregistered = KittyUnicodePlaceholder.decode(
            line, 2, 4, TextStyle.decodeForeColor(getStyleAt(0, 1)),
            TextStyle.decodeBackColor(getStyleAt(0, 1)), TextStyle.COLOR_INDEX_FOREGROUND, null);
        assertEquals(4, unregistered.placementId);
        assertFalse(mTerminal.getKittyImagePlaceholder(5, 4, new KittyImagePlaceholder()));
        enterString("\033[0;38;2;0;0;5m"); // Reset, then fg = image id 5, default bg.
        enterString(new String(cell));
        KittyUnicodePlaceholder.Cell defaultBg = KittyUnicodePlaceholder.decode(
            line, 2, 4, TextStyle.decodeForeColor(getStyleAt(0, 2)),
            TextStyle.decodeBackColor(getStyleAt(0, 2)), TextStyle.COLOR_INDEX_FOREGROUND, null);
        assertEquals(0, defaultBg.placementId);
        KittyImagePlaceholder fallback = new KittyImagePlaceholder();
        assertTrue(mTerminal.getKittyImagePlaceholder(5, 0, fallback));
        assertEquals(1, fallback.columns); // the first virtual placement, the p=3 c=1 one
    }

    public void testUnicodePlacementModeTwoIsUnsupported() {
        enterString(apc("a=t,i=5,U=2,f=24,s=1,v=1;" + base64(1, 2, 3)));
        assertEquals(reply("i=5", "EINVAL:unsupported unicode placeholder mode"), mOutput.getOutputAndClear());
    }

    public void testFailedDecodeLeavesNoResolvablePlaceholder() {
        // With the decode seam removed the platform decoder fails, and the whole placement
        // attempt must unwind: the reservation, the registered virtual placement and the
        // stored image all go with it.
        kitty.decodeForTests = null;
        enterString(apc("a=T,U=1,i=1,p=3,f=32,s=1,v=1;" + base64(1, 2, 3, 4)));
        assertEquals(reply("i=1,p=3", "EINVAL:image decode failed"), mOutput.getOutputAndClear());
        assertFalse(mTerminal.hasKittyVirtualPlacement(1, 3));
        assertFalse(mTerminal.getKittyImagePlaceholder(1, 3, new KittyImagePlaceholder()));
        // Observable proof the image is gone: a later put finds nothing to place.
        enterString(apc("a=p,i=1"));
        assertEquals(reply("i=1", "ENOENT:image not found"), mOutput.getOutputAndClear());
    }

    public void testStreamedPayloadRejectsInvalidBase64() {
        // '!' is not base64: the command is refused mid-stream and the rest of the payload is
        // absorbed silently up to the string terminator.
        enterString(apc("a=T,i=7,f=24,s=1,v=1;!!" + base64(1, 2, 3)));
        assertEquals(reply("i=7", "EINVAL:invalid base64 payload"), mOutput.getOutputAndClear());
        assertEquals(0, placements(7).size());
        assertCursorAt(0, 0);
    }

    public void testStreamedPayloadRejectsLengthNotAMultipleOfFour() {
        // Five base64 characters carry 30 bits, which is not a whole number of bytes.
        enterString(apc("a=T,i=7,f=24,s=1,v=1;AAAAA"));
        assertEquals(reply("i=7", "EINVAL:invalid base64 payload"), mOutput.getOutputAndClear());
    }

    public void testStreamedPayloadRejectsNonZeroTrailingBits() {
        // Two characters carry 12 bits; the trailing 4 must be zero, and 'AB' ends in 01.
        enterString(apc("a=T,i=7,f=24,s=1,v=1;AB"));
        assertEquals(reply("i=7", "EINVAL:invalid base64 payload"), mOutput.getOutputAndClear());
    }

    public void testStreamedPayloadRejectsDataAfterPadding() {
        enterString(apc("a=T,i=7,f=100;AA=A"));
        assertEquals(reply("i=7", "EINVAL:invalid base64 payload"), mOutput.getOutputAndClear());
    }

    public void testFileTransmissionReadsTheNamedFile() throws Exception {
        File file = File.createTempFile("kitty-protocol-test", ".png");
        try {
            Files.write(file.toPath(), pngHeader(2, 3));
            enterString(apc("a=T,i=7,t=f,f=100;"
                + base64(file.getPath().getBytes(StandardCharsets.UTF_8))));
            assertEquals(reply("i=7", "OK"), mOutput.getOutputAndClear());
            assertEquals(1, placements(7).size());
            assertEquals(2, placements(7).get(0).getWidth());
        } finally {
            file.delete();
        }
    }

    public void testFileTransmissionOfAMissingFileAnswersEbadf() {
        enterString(apc("a=T,i=7,t=f,f=100;"
            + base64("/nonexistent/kitty-protocol-test.png".getBytes(StandardCharsets.UTF_8))));
        assertEquals(reply("i=7", "EBADF:cannot read transmission file"), mOutput.getOutputAndClear());
    }

    public void testFileTransmissionNeedsAPath() {
        enterString(apc("a=T,i=7,t=f,f=100;"));
        assertEquals(reply("i=7", "EINVAL:file transmission requires a path"), mOutput.getOutputAndClear());
    }
}
