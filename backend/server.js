const express = require("express");
const axios = require("axios");
const cors = require("cors");

const app = express();

const PORT =
    process.env.PORT || 3000;

const PRESENCE_URL =
    "https://whatamilistening.com/api/presence";


app.use(express.json());


app.use(
    cors({
        origin: [
            "https://whatamilistening.com",
            "https://www.whatamilistening.com"
        ]
    })
);


let currentMusic = {
    song: null,
    artist: null,
    app: null,
    artwork: null,
    playing: false,
    lastUpdated: null
};


const artworkCache =
    new Map();


async function getArtwork(
    song,
    artist
) {

    if (!song || !artist) {
        return null;
    }


    const cacheKey =
        `${song.trim().toLowerCase()}|${artist.trim().toLowerCase()}`;


    if (
        artworkCache.has(
            cacheKey
        )
    ) {

        return artworkCache.get(
            cacheKey
        );
    }


    try {

        const query =
            encodeURIComponent(
                `${song} ${artist}`
            );


        const response =
            await axios.get(
                `https://itunes.apple.com/search?term=${query}&entity=song&limit=1`,
                {
                    timeout: 5000
                }
            );


        const result =
            response.data.results?.[0];


        if (
            !result?.artworkUrl100
        ) {

            artworkCache.set(
                cacheKey,
                null
            );

            return null;
        }


        const artwork =
            result.artworkUrl100.replace(
                "100x100",
                "600x600"
            );


        artworkCache.set(
            cacheKey,
            artwork
        );


        return artwork;


    } catch (error) {

        console.log(
            "Artwork lookup failed:",
            error.message
        );


        return null;
    }
}


async function updatePresence(
    playing
) {

    try {

        await axios.post(
            PRESENCE_URL,
            {
                playing:
                    playing === true
            },
            {
                timeout: 5000
            }
        );


        console.log(
            "Presence updated:",
            playing
        );


    } catch (error) {

        console.log(
            "Presence update failed:",
            error.message
        );
    }
}


app.post(
    "/api/update",

    async (req, res) => {

        try {

            const {
                song,
                artist,
                app: musicApp,
                playing
            } = req.body;


            /*
            Allow empty song details when
            playing=false.

            This is important when you pause
            sharing.
            */

            if (
                playing === true &&
                (
                    !song ||
                    typeof song !== "string"
                )
            ) {

                return res
                    .status(400)
                    .json({
                        success: false,
                        message:
                            "Song title is required while playing"
                    });
            }


            /*
            If music is playing, look up artwork.

            If offline, keep artwork null.
            */

            const artwork =

                playing === true

                    ? await getArtwork(
                        song,
                        artist
                    )

                    : null;


            currentMusic = {

                song:
                    typeof song === "string"
                        ? song.trim()
                        : null,


                artist:
                    typeof artist === "string" &&
                    artist.trim()

                        ? artist.trim()

                        : null,


                app:
                    typeof musicApp === "string" &&
                    musicApp.trim()

                        ? musicApp.trim()

                        : null,


                artwork: artwork,


                playing:
                    playing === true,


                lastUpdated:
                    Date.now()
            };


            /*
            Save online/offline presence
            separately on Cloudflare.
            */

            await updatePresence(
                currentMusic.playing
            );


            console.log(
                "Music updated:",
                currentMusic
            );


            res.json({
                success: true,
                music: currentMusic
            });


        } catch (error) {

            console.error(
                "Update failed:",
                error
            );


            res.status(500).json({
                success: false,
                message:
                    "Could not update music status"
            });
        }
    }
);


app.get(
    "/api/status",

    (req, res) => {

        res.json(
            currentMusic
        );
    }
);


app.listen(
    PORT,
    "0.0.0.0",

    () => {

        console.log(
            `Server running on port ${PORT}`
        );
    }
);