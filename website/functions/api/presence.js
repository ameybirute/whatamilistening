export async function onRequestGet(context) {
    const status = await context.env.MUSIC_STATUS.get(
        "presence",
        {
            type: "json"
        }
    );

    return Response.json(
        status || {
            playing: false,
            lastSeen: null
        },
        {
            headers: {
                "Cache-Control": "no-store"
            }
        }
    );
}

export async function onRequestPost(context) {

    try {

        const body =
            await context.request.json();

        const playing =
            body.playing === true;

        const previous =
            await context.env.MUSIC_STATUS.get(
                "presence",
                {
                    type: "json"
                }
            );

        const now =
            Date.now();

        const presence = {
            playing: playing,

            lastSeen:
                playing
                    ? previous?.lastSeen || now
                    : now
        };

        await context.env.MUSIC_STATUS.put(
            "presence",
            JSON.stringify(presence)
        );

        return Response.json({
            success: true,
            presence: presence
        });

    } catch (error) {

        return Response.json(
            {
                success: false,
                message: "Could not update presence"
            },
            {
                status: 400
            }
        );
    }
}