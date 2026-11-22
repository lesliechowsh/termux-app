package com.termux.terminal;

/**
 * The a=d delete forms: which specifier removes which placements, when the stored image goes
 * with them, and the errors each underspecified form earns.
 */
public class KittyGraphicsDeleteTest extends KittyGraphicsTestCase {

    /**
     * A successful delete only answers when the command addressed an image (the reply then
     * carries its id); an id-less form succeeds silently. Its errors are always answered.
     */
    private void assertSilentOk() {
        assertEquals("", mOutput.getOutputAndClear());
    }

    private void storeAndPlace(long id) {
        enterString(apc("a=t,i=" + id + ",f=24,s=1,v=1;" + base64(1, 2, 3)));
        mOutput.getOutputAndClear();
        enterString(apc("a=p,i=" + id));
        mOutput.getOutputAndClear();
    }

    public void testLowercaseDeleteRemovesPlacementButKeepsStoredImage() {
        storeAndPlace(7);
        enterString(apc("a=d,d=i,i=7"));
        assertEquals(reply("i=7", "OK"), mOutput.getOutputAndClear());
        assertEquals(0, placements(7).size());
        assertEquals(1, kitty.store().count());
    }

    public void testUppercaseDeleteAlsoFreesTheStoredImage() {
        storeAndPlace(7);
        enterString(apc("a=d,d=I,i=7"));
        assertEquals(reply("i=7", "OK"), mOutput.getOutputAndClear());
        assertEquals(0, placements(7).size());
        assertEquals(0, kitty.store().count());
    }

    public void testDeleteAllClearsEveryPlacement() {
        storeAndPlace(7);
        storeAndPlace(8);
        enterString(apc("a=d,d=a"));
        assertSilentOk();
        assertEquals(0, placements(7).size());
        assertEquals(0, placements(8).size());
        assertEquals(2, kitty.store().count());
    }

    public void testUppercaseDeleteAllAlsoFreesUnplacedImages() {
        enterString(apc("a=t,i=7,f=24,s=1,v=1;" + base64(1, 2, 3)));
        mOutput.getOutputAndClear();
        enterString(apc("a=t,i=8,f=24,s=1,v=1;" + base64(1, 2, 3)));
        mOutput.getOutputAndClear();
        assertEquals(2, kitty.store().count());
        enterString(apc("a=d,d=A"));
        assertSilentOk();
        assertEquals(0, kitty.store().count());
        // Observable proof the store is empty: a later put finds nothing to place.
        enterString(apc("a=p,i=7"));
        assertEquals(reply("i=7", "ENOENT:image not found"), mOutput.getOutputAndClear());
    }

    public void testDeleteAllWithImageIdDeletesOnlyThatImage() {
        // d=a plus an explicit i= reads as delete-by-id — this fork's documented reading of the
        // legacy form, not kitty's "delete everything" reading.
        storeAndPlace(7);
        storeAndPlace(8);
        enterString(apc("a=d,d=a,i=7"));
        assertEquals(reply("i=7", "OK"), mOutput.getOutputAndClear());
        assertEquals(0, placements(7).size());
        assertEquals(1, placements(8).size());
    }

    public void testDeleteAtCursorRemovesThePlacementUnderIt() {
        storeAndPlace(7);         // covers (0,0); the cursor moved past it to (0,1)
        enterString("\033[1;1H"); // back inside the image
        enterString(apc("a=d,d=c"));
        assertSilentOk();
        assertEquals(0, placements(7).size());
    }

    public void testDeleteAtCursorSparesOtherCells() {
        storeAndPlace(7); // covers (0,0); the cursor sits at (0,1)
        enterString(apc("a=d,d=c"));
        assertSilentOk();
        assertEquals(1, placements(7).size());
    }

    public void testDeleteByColumnRequiresX() {
        enterString(apc("a=d,d=x"));
        assertEquals(reply("", "EINVAL:delete by column requires x"), mOutput.getOutputAndClear());
    }

    public void testDeleteByRowRequiresY() {
        enterString(apc("a=d,d=y"));
        assertEquals(reply("", "EINVAL:delete by row requires y"), mOutput.getOutputAndClear());
    }

    public void testDeleteByZRequiresZ() {
        enterString(apc("a=d,d=z"));
        assertEquals(reply("", "EINVAL:delete by z requires z"), mOutput.getOutputAndClear());
    }

    public void testDeleteByPositionRequiresXAndY() {
        enterString(apc("a=d,d=p"));
        assertEquals(reply("", "EINVAL:delete by position requires x and y"), mOutput.getOutputAndClear());
    }

    public void testDeleteByNumberRequiresImageNumber() {
        enterString(apc("a=d,d=n"));
        assertEquals(reply("", "EINVAL:delete by number requires I"), mOutput.getOutputAndClear());
    }

    public void testDeleteByIdRequiresImageId() {
        enterString(apc("a=d,d=i"));
        assertEquals(reply("", "EINVAL:delete by id requires i"), mOutput.getOutputAndClear());
    }

    public void testUnknownDeleteSpecifierIsAnswered() {
        enterString(apc("a=d,d=w"));
        assertEquals(reply("", "EINVAL:unknown delete specifier"), mOutput.getOutputAndClear());
    }

    public void testDeletingAnUnknownImageStillAnswersOk() {
        // Deleting a missing image is done, not an error — kitty's reading.
        enterString(apc("a=d,d=i,i=99"));
        assertEquals(reply("i=99", "OK"), mOutput.getOutputAndClear());
    }

    public void testControlOnlyDeleteFormIsAccepted() {
        // The ';' separator is optional: the canonical delete command carries no payload at all.
        storeAndPlace(7);
        enterString("\033_Ga=d,d=a\033\\");
        assertSilentOk();
        assertEquals(0, placements(7).size());
    }

    public void testDeleteByColumnSparesImagesInOtherColumns() {
        storeAndPlace(7); // a 1x1 cell image, covering column 0 only
        enterString(apc("a=d,d=x,x=2")); // x is one-based: column index 1
        assertSilentOk();
        assertEquals(1, placements(7).size());
        enterString(apc("a=d,d=x,x=1"));
        assertSilentOk();
        assertEquals(0, placements(7).size());
    }

    public void testDeleteByIdRemovesVirtualPlacements() {
        enterString(apc("a=T,i=5,U=1,f=24,s=1,v=1;" + base64(1, 2, 3)));
        mOutput.getOutputAndClear();
        assertTrue(mTerminal.hasKittyVirtualPlacement(5, 0));
        enterString(apc("a=d,d=i,i=5"));
        assertEquals(reply("i=5", "OK"), mOutput.getOutputAndClear());
        assertFalse(mTerminal.hasKittyVirtualPlacement(5, 0));
    }

    public void testUppercaseFrameDeleteRemovesFramelessImage() {
        // d=f on an image without extra frames is a no-op; d=F frees the whole image.
        enterString(apc("a=t,i=7,f=24,s=1,v=1;" + base64(1, 2, 3)));
        mOutput.getOutputAndClear();
        enterString(apc("a=d,d=f,i=7"));
        assertEquals(reply("i=7", "OK"), mOutput.getOutputAndClear());
        assertEquals(1, kitty.store().count());
        enterString(apc("a=d,d=F,i=7"));
        assertEquals(reply("i=7", "OK"), mOutput.getOutputAndClear());
        assertEquals(0, kitty.store().count());
    }
}
