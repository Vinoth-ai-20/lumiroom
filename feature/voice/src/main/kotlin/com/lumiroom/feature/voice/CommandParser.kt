package com.lumiroom.feature.voice

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses a raw speech transcript into a structured [VoiceCommand].
 * Deterministic rule-based parsing matching specific patterns.
 */
@Singleton
class CommandParser @Inject constructor() {

    // Help
    private val helpPattern = Regex("""(?:help|show commands|what can i say)""")

    // Placement
    private val placePattern = Regex("""(?:place|add|put)\s+(?:a\s+)?(?:the\s+)?(.+)""")

    // Selection
    private val selectLastPattern = Regex("""select\s+(?:the\s+)?last\s+item""")
    private val deselectPattern = Regex("""deselect\s+(?:item|all)?""")
    private val selectPattern = Regex("""select\s+(?:a\s+)?(?:the\s+)?(.+)""")

    // Manipulation
    // Manipulation
    private val rotateAbsolutePattern = Regex("""rotate\s+(?:it\s+)?to\s+(\d+)\s*(?:degrees?)?""")
    private val rotatePattern = Regex("""rotate\s+(?:it\s+)?(?:left|right|clockwise|counter\s*clockwise)?\s*(?:by\s+)?(\d+)\s*(?:degrees?)?""")
    private val rotateSimplePattern = Regex("""rotate\s+(left|right)""")
    private val scaleAbsolutePattern = Regex("""scale\s+(?:it\s+)?to\s+(\d+(?:\.\d+)?)(?:%)?""")
    private val scaleRelativePattern = Regex("""(?:increase|decrease|increase\s+size\s+by|decrease\s+size\s+by)\s+(?:it\s+)?(?:by\s+)?(\d+(?:\.\d+)?)(?:%)?""")
    private val scaleUpPattern = Regex("""(?:scale|make|size)\s+(?:it\s+)?(?:up|bigger|larger)""")
    private val scaleDownPattern = Regex("""(?:scale|make|size)\s+(?:it\s+)?(?:down|smaller)""")
    private val movePattern = Regex("""move\s+(?:the\s+)?(?:[a-zA-Z]+\s+)?(forward|backward|left|right)""")

    // Editing
    private val deleteSelectedPattern = Regex("""(?:delete|remove)\s+(?:selected|it)""")
    private val removePattern = Regex("""(?:remove|delete|take away)\s+(?:the\s+)?(.+)""")
    private val replacePattern = Regex("""replace\s+(?:furniture|item|with\s+)?(.+)""")
    private val hideObjectPattern = Regex("""(?:hide|invisible)\s+(?:the\s+)?(?:object|item|selected)""")
    private val lockObjectPattern = Regex("""(?:lock|freeze)\s+(?:the\s+)?(?:object|item|selected)""")
    private val resetObjectPattern = Regex("""(?:reset|restore)\s+(?:the\s+)?(?:object|item|selected)""")

    // Room Management
    private val saveRoomPattern = Regex("""save\s+(?:the\s+)?room""")
    private val loadRoomPattern = Regex("""load\s+(?:the\s+)?room""")
    private val createRoomPattern = Regex("""(?:create|new)\s+(?:the\s+)?room""")
    private val deleteRoomPattern = Regex("""delete\s+(?:the\s+)?room""")

    // Catalog
    private val openCatalogPattern = Regex("""(?:open|show)\s+(?:the\s+)?catalog""")
    private val closeCatalogPattern = Regex("""(?:close|hide)\s+(?:the\s+)?catalog""")
    private val showCategoryPattern = Regex("""show\s+(sofas|chairs|tables|beds|cabinets|shelves|decor)""")
    private val searchCatalogPattern = Regex("""search\s+(?:for\s+)?(.+)""")

    // View Controls
    private val showPlanesPattern = Regex("""show\s+planes""")
    private val hidePlanesPattern = Regex("""hide\s+planes""")
    private val focusSelectedPattern = Regex("""focus\s+(?:selected|it)?""")
    private val resetCameraPattern = Regex("""reset\s+camera""")

    // Capture
    private val takeScreenshotPattern = Regex("""take\s+(?:a\s+)?screenshot""")
    private val shareScreenshotPattern = Regex("""share\s+(?:the\s+)?screenshot""")

    // Others
    private val undoPattern = Regex("""undo""")
    private val redoPattern = Regex("""redo""")
    private val clearPattern = Regex("""(?:clear|reset|empty)\s+(?:the\s+)?(?:room|scene|all)""")

    fun parse(transcript: String): VoiceCommand {
        val normalized = transcript.trim().lowercase()

        // Help
        helpPattern.find(normalized)?.let { return VoiceCommand.Help }

        // Capture
        takeScreenshotPattern.find(normalized)?.let { return VoiceCommand.TakeScreenshot }
        shareScreenshotPattern.find(normalized)?.let { return VoiceCommand.ShareScreenshot }

        // View Controls
        showPlanesPattern.find(normalized)?.let { return VoiceCommand.ShowPlanes }
        hidePlanesPattern.find(normalized)?.let { return VoiceCommand.HidePlanes }
        focusSelectedPattern.find(normalized)?.let { return VoiceCommand.FocusSelected }
        resetCameraPattern.find(normalized)?.let { return VoiceCommand.ResetCamera }

        // Room Management
        saveRoomPattern.find(normalized)?.let { return VoiceCommand.SaveRoom }
        loadRoomPattern.find(normalized)?.let { return VoiceCommand.LoadRoom }
        createRoomPattern.find(normalized)?.let { return VoiceCommand.CreateRoom }
        deleteRoomPattern.find(normalized)?.let { return VoiceCommand.DeleteRoom }

        // Catalog
        openCatalogPattern.find(normalized)?.let { return VoiceCommand.OpenCatalog }
        closeCatalogPattern.find(normalized)?.let { return VoiceCommand.CloseCatalog }
        showCategoryPattern.find(normalized)?.let { match ->
            return VoiceCommand.SearchCatalog(match.groupValues[1].trim())
        }
        searchCatalogPattern.find(normalized)?.let { match ->
            return VoiceCommand.SearchCatalog(match.groupValues[1].trim())
        }

        // Editing
        deleteSelectedPattern.find(normalized)?.let { return VoiceCommand.DeleteSelected }
        hideObjectPattern.find(normalized)?.let { return VoiceCommand.HideObject }
        lockObjectPattern.find(normalized)?.let { return VoiceCommand.LockObject }
        resetObjectPattern.find(normalized)?.let { return VoiceCommand.ResetObject }
        replacePattern.find(normalized)?.let { match ->
            return VoiceCommand.Replace(match.groupValues[1].trim())
        }

        // Manipulation
        scaleAbsolutePattern.find(normalized)?.let { match ->
            val percent = match.groupValues[1].toFloatOrNull() ?: 100f
            return VoiceCommand.ScaleAbsolute(percent / 100f)
        }
        scaleRelativePattern.find(normalized)?.let { match ->
            val amountStr = match.groupValues[1]
            val isDecrease = normalized.contains("decrease")
            val amount = amountStr.toFloatOrNull() ?: 10f
            // Relative amount (e.g. 10% = 0.1)
            val delta = amount / 100f
            val factor = if (isDecrease) -delta else delta
            return VoiceCommand.ScaleRelative(factor)
        }
        scaleUpPattern.find(normalized)?.let { return VoiceCommand.ScaleRelative(0.1f) }
        scaleDownPattern.find(normalized)?.let { return VoiceCommand.ScaleRelative(-0.1f) }
        
        movePattern.find(normalized)?.let { match ->
            return VoiceCommand.Move(match.groupValues[1].trim())
        }
        
        rotateAbsolutePattern.find(normalized)?.let { match ->
            val degrees = match.groupValues[1].toFloatOrNull() ?: 0f
            return VoiceCommand.RotateAbsolute(degrees)
        }
        rotatePattern.find(normalized)?.let { match ->
            val degrees = match.groupValues[1].toFloatOrNull() ?: 15f
            val isLeft = normalized.contains("left") || normalized.contains("counter")
            val angle = if (isLeft) -degrees else degrees
            return VoiceCommand.RotateRelative(angle)
        }
        rotateSimplePattern.find(normalized)?.let { match ->
            val direction = match.groupValues[1]
            val angle = if (direction == "left") -15f else 15f
            return VoiceCommand.RotateRelative(angle)
        }

        // Selection
        selectLastPattern.find(normalized)?.let { return VoiceCommand.SelectLast }
        deselectPattern.find(normalized)?.let { return VoiceCommand.Deselect }
        selectPattern.find(normalized)?.let { match ->
            return VoiceCommand.Select(match.groupValues[1].trim())
        }

        // Other actions
        undoPattern.find(normalized)?.let { return VoiceCommand.Undo }
        redoPattern.find(normalized)?.let { return VoiceCommand.Redo }
        clearPattern.find(normalized)?.let { return VoiceCommand.ClearScene }

        // Fallbacks (place/remove can match heavily, keep at bottom)
        removePattern.find(normalized)?.let { match ->
            return VoiceCommand.Remove(match.groupValues[1].trim())
        }
        placePattern.find(normalized)?.let { match ->
            return VoiceCommand.Place(match.groupValues[1].trim())
        }

        // Fuzzy Keyword Fallbacks
        if (normalized.contains("hide") && (normalized.contains("object") || normalized.contains("item"))) return VoiceCommand.HideObject
        if (normalized.contains("lock") && (normalized.contains("object") || normalized.contains("item"))) return VoiceCommand.LockObject
        if (normalized.contains("reset") && (normalized.contains("object") || normalized.contains("item"))) return VoiceCommand.ResetObject
        if (normalized.contains("delete") && (normalized.contains("object") || normalized.contains("item") || normalized.contains("selected"))) return VoiceCommand.DeleteSelected
        if (normalized.contains("save") && normalized.contains("room")) return VoiceCommand.SaveRoom
        if (normalized.contains("load") && normalized.contains("room")) return VoiceCommand.LoadRoom

        return VoiceCommand.Unknown(transcript)
    }
}

sealed class VoiceCommand {
    object Help : VoiceCommand()

    data class Place(val itemName: String) : VoiceCommand()
    data class Remove(val itemName: String) : VoiceCommand()
    data class Replace(val itemName: String) : VoiceCommand()

    data class Select(val itemName: String) : VoiceCommand()
    object SelectLast : VoiceCommand()
    object Deselect : VoiceCommand()

    data class RotateRelative(val degrees: Float) : VoiceCommand()
    data class RotateAbsolute(val degrees: Float) : VoiceCommand()
    data class ScaleRelative(val delta: Float) : VoiceCommand()
    data class ScaleAbsolute(val percent: Float) : VoiceCommand()
    data class Move(val direction: String) : VoiceCommand()

    object DeleteSelected : VoiceCommand()
    object HideObject : VoiceCommand()
    object LockObject : VoiceCommand()
    object ResetObject : VoiceCommand()

    object SaveRoom : VoiceCommand()
    object LoadRoom : VoiceCommand()
    object CreateRoom : VoiceCommand()
    object DeleteRoom : VoiceCommand()

    object OpenCatalog : VoiceCommand()
    object CloseCatalog : VoiceCommand()
    data class SearchCatalog(val query: String) : VoiceCommand()

    object ShowPlanes : VoiceCommand()
    object HidePlanes : VoiceCommand()
    object FocusSelected : VoiceCommand()
    object ResetCamera : VoiceCommand()

    object TakeScreenshot : VoiceCommand()
    object ShareScreenshot : VoiceCommand()

    object Undo : VoiceCommand()
    object Redo : VoiceCommand()
    object ClearScene : VoiceCommand()

    data class Unknown(val transcript: String) : VoiceCommand()
}
