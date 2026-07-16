const cors = require("cors");
const express = require("express");
const axios = require("axios");

const app = express();
app.use(cors({
    origin: "https://whatamilistening.com"
}));
const PORT = process.env.PORT || 3000;


app.use(express.json());

let currentMusic = {
    song: null,
    artist: null,
    app: null,
    artwork: null,
    playing: false,
    lastUpdated: null
};


const artworkCache = new Map();

async function getArtwork(song, artist) {
    if (!song || !artist) {
        return null;
    }

    const cacheKey =
        `${song.trim().toLowerCase()}|${artist.trim().toLowerCase()}`;

    if (artworkCache.has(cacheKey)) {
        return artworkCache.get(cacheKey);
    }

    try {
        const query = encodeURIComponent(`${song} ${artist}`);

        const response = await axios.get(
            `https://itunes.apple.com/search?term=${query}&entity=song&limit=1`,
            {
                timeout: 5000
            }
        );

        const result = response.data.results?.[0];

        if (!result?.artworkUrl100) {
            artworkCache.set(cacheKey, null);
            return null;
        }

        const artwork = result.artworkUrl100.replace(
            "100x100",
            "600x600"
        );

        artworkCache.set(cacheKey, artwork);

        return artwork;

    } catch (error) {
        console.log("Artwork lookup failed:", error.message);
        return null;
    }
}


app.post("/api/update", async (req, res) => {
    try {
        const {
            song,
            artist,
            app: musicApp,
            playing
        } = req.body;

        if (!song || typeof song !== "string") {
            return res.status(400).json({
                success: false,
                message: "Song title is required"
            });
        }

        const artwork = await getArtwork(song, artist);

        currentMusic = {
            song: song.trim(),
            artist:
                typeof artist === "string" && artist.trim()
                    ? artist.trim()
                    : "Unknown artist",
            app:
                typeof musicApp === "string" && musicApp.trim()
                    ? musicApp.trim()
                    : "Unknown app",
            artwork,
            playing: playing === true,
            lastUpdated: Date.now()
        };

        console.log(
            `Music updated: ${currentMusic.song} - ${currentMusic.artist}`
        );

        res.json({
            success: true,
            music: currentMusic
        });

    } catch (error) {
        console.error("Update failed:", error);

        res.status(500).json({
            success: false,
            message: "Could not update music status"
        });
    }
});


app.get("/api/status", (req, res) => {
    res.json(currentMusic);
});


app.listen(PORT, "0.0.0.0", () => {
    console.log(`Website: http://localhost:${PORT}`);
    console.log(`API: http://localhost:${PORT}/api/status`);
});