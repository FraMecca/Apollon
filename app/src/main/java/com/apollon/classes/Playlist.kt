package com.apollon.classes

import java.io.Serializable

class Playlist( val id: String, val title: String, val img_url: String) : Serializable {
    constructor(id: String, title: String) : this(id, title, "https://cdn3.iconfinder.com/data/icons/66-cds-dvds/512/Icon_60-512.png")
}