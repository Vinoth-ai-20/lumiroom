package com.lumiroom.feature.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class CommandParserTest {

    private val parser = CommandParser()

    @Test
    fun testPlacementCommands() {
        assertEquals(VoiceCommand.Place("sofa"), parser.parse("place sofa"))
        assertEquals(VoiceCommand.Place("chair"), parser.parse("add a chair"))
        assertEquals(VoiceCommand.Place("modern table"), parser.parse("put the modern table"))
    }

    @Test
    fun testSelectionCommands() {
        assertEquals(VoiceCommand.Select("sofa"), parser.parse("select sofa"))
        assertEquals(VoiceCommand.SelectLast, parser.parse("select the last item"))
        assertEquals(VoiceCommand.Deselect, parser.parse("deselect item"))
        assertEquals(VoiceCommand.Deselect, parser.parse("deselect all"))
    }

    @Test
    fun testManipulationCommands() {
        assertEquals(VoiceCommand.Rotate(15f), parser.parse("rotate right"))
        assertEquals(VoiceCommand.Rotate(-15f), parser.parse("rotate left"))
        assertEquals(VoiceCommand.Rotate(45f), parser.parse("rotate right 45 degrees"))
        
        assertEquals(VoiceCommand.Scale(1.1f), parser.parse("scale up"))
        assertEquals(VoiceCommand.Scale(0.9f), parser.parse("scale it down"))

        assertEquals(VoiceCommand.Move("forward"), parser.parse("move forward"))
        assertEquals(VoiceCommand.Move("left"), parser.parse("move left"))
    }

    @Test
    fun testEditingCommands() {

        
        assertEquals(VoiceCommand.DeleteSelected, parser.parse("delete selected"))
        assertEquals(VoiceCommand.Remove("sofa"), parser.parse("remove the sofa"))
        
        assertEquals(VoiceCommand.Replace("chair"), parser.parse("replace furniture chair"))
    }

    @Test
    fun testRoomManagement() {
        assertEquals(VoiceCommand.SaveRoom, parser.parse("save room"))
        assertEquals(VoiceCommand.LoadRoom, parser.parse("load the room"))
        assertEquals(VoiceCommand.CreateRoom, parser.parse("create new room"))
    }

    @Test
    fun testCatalogCommands() {
        assertEquals(VoiceCommand.OpenCatalog, parser.parse("open the catalog"))
        assertEquals(VoiceCommand.CloseCatalog, parser.parse("close catalog"))
        assertEquals(VoiceCommand.SearchCatalog("sofas"), parser.parse("show sofas"))
        assertEquals(VoiceCommand.SearchCatalog("modern sofa"), parser.parse("search for modern sofa"))
    }

    @Test
    fun testViewControls() {
        assertEquals(VoiceCommand.ShowPlanes, parser.parse("show planes"))
        assertEquals(VoiceCommand.HidePlanes, parser.parse("hide planes"))
        assertEquals(VoiceCommand.FocusSelected, parser.parse("focus selected"))
        assertEquals(VoiceCommand.ResetCamera, parser.parse("reset camera"))
    }

    @Test
    fun testCaptureCommands() {
        assertEquals(VoiceCommand.TakeScreenshot, parser.parse("take a screenshot"))
        assertEquals(VoiceCommand.ShareScreenshot, parser.parse("share screenshot"))
    }
}
