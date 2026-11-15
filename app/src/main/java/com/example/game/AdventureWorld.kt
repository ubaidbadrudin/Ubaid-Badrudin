package com.example.game

import com.example.R

object AdventureWorld {

  val avatarOptions = listOf("🧭", "🦊", "🦉", "🛡️", "🌿", "⭐")

  val shopItems =
    listOf(
      ShopItem(
        itemKey = "berry_sweet",
        name = "Beri Manis Rimba",
        description = "Memulihkan 10 HP dan +4 Energi. Cocok untuk perjalanan santai.",
        category = "snack",
        priceCoins = 10L,
        healAmount = 10L,
        energyAmount = 4L,
        iconKey = "snack",
      ),
      ShopItem(
        itemKey = "warm_bread",
        name = "Roti Hangat Kemah",
        description = "Memulihkan +8 Energi agar kamu bisa terus menjelajah.",
        category = "snack",
        priceCoins = 15L,
        healAmount = 4L,
        energyAmount = 8L,
        iconKey = "bread",
      ),
      ShopItem(
        itemKey = "potion_herb",
        name = "Ramuan Herbal",
        description = "Memulihkan 18 HP dengan cepat saat bertemu penjaga wilayah.",
        category = "potion",
        priceCoins = 20L,
        healAmount = 18L,
        energyAmount = 0L,
        iconKey = "potion",
      ),
    )

  val regions =
    listOf(
      RegionInfo(
        id = "forest",
        name = "Hutan Bisikan",
        subtitle = "Langkah awal yang sejuk dan bersahabat untuk petualang pemula.",
        difficultyLabel = "Ramah Pemula",
        recommendedLevel = 1,
        startNodeId = "forest_start",
        nodeIds = listOf("forest_start", "forest_stream", "forest_guardian", "forest_shrine"),
        imageRes = R.drawable.img_region_forest,
        rewardSummary = "Lentera Emas, Ramuan & +90 XP",
      ),
      RegionInfo(
        id = "ruins",
        name = "Reruntuhan Kuno",
        subtitle = "Lorong batu penuh cahaya rune biru dan teka-teki sejarah.",
        difficultyLabel = "Teka-Teki",
        recommendedLevel = 2,
        startNodeId = "ruins_gate",
        nodeIds = listOf("ruins_gate", "ruins_hall", "ruins_golem", "ruins_sanctum"),
        imageRes = R.drawable.img_region_ruins,
        rewardSummary = "Jimat Burung Hantu & +125 XP",
      ),
      RegionInfo(
        id = "summit",
        name = "Puncak Kristal",
        subtitle = "Puncak berkilau di atas awan tempat suar bintang dinyalakan.",
        difficultyLabel = "Puncak Cerita",
        recommendedLevel = 3,
        startNodeId = "summit_trail",
        nodeIds = listOf("summit_trail", "summit_bridge", "summit_dragon", "summit_beacon"),
        imageRes = R.drawable.img_region_summit,
        rewardSummary = "Lencana Petualang Utama & +185 XP",
      ),
    )

  private val nodes: Map<String, StoryNode> =
    listOf(
        // --- WILAYAH 1: HUTAN BISIKAN ---
        StoryNode(
          id = "forest_start",
          regionId = "forest",
          title = "Gerbang Hutan Bisikan",
          speaker = "Pip si Burung Hantu",
          pipTip =
            "Tips Pemula: Pilih tombol di bawah untuk melangkah. Pilihan berlabel [Aman] tidak memakai Energi!",
          narrative =
            "Cahaya pagi menembus dedaunan hijau. Pip hinggap di papan kayu tua sambil mengepakkan sayap kecilnya.\n\n\"Selamat datang, petualang baru! Di depan ada dua jalur menuju Sungai Berkilau. Kamu ingin berjalan santai atau mencari bekal dulu?\"",
          choices =
            listOf(
              StoryChoice(
                id = "forest_start_path",
                label = "Ikuti Jalan Setapak Berbatu",
                badge = "Aman • +10 XP",
                energyCost = 0,
                xpReward = 10,
                coinReward = 8,
                nextNodeId = "forest_stream",
                resultMessage = "Kamu menyusuri jalan setapak yang rapi dan menemukan 8 koin emas di tepi batu!",
              ),
              StoryChoice(
                id = "forest_start_berry",
                label = "Cari Beri di Semak Berkilau",
                badge = "Dapat Item • -1 Energi",
                energyCost = 1,
                xpReward = 15,
                coinReward = 5,
                nextNodeId = "forest_stream",
                resultMessage = "Wah! Kamu memetik Beri Manis Rimba yang segar dan menyimpannya ke tas.",
                itemReward =
                  ItemReward(
                    itemKey = "berry_sweet",
                    name = "Beri Manis Rimba",
                    description = "Camilan segar pemula yang memulihkan 10 HP dan 4 Energi.",
                    category = "snack",
                    quantity = 1L,
                    healAmount = 10L,
                    energyAmount = 4L,
                    iconKey = "snack",
                  ),
              ),
            ),
        ),
        StoryNode(
          id = "forest_stream",
          regionId = "forest",
          title = "Tepi Sungai Berkilau",
          speaker = "Penjaga Peri Air",
          pipTip =
            "Tips Pemula: Teka-teki di game ini selalu punya petunjuk gratis. Jangan takut mencoba menjawab!",
          narrative =
            "Air sungai jernih mengalir memantulkan warna langit. Di tengah jembatan kayu, seekor peri air mungil melambai ramah.\n\n\"Kalau ingin lewat dengan hadiah koin, jawab teka-teki sederhanaku! Atau bantu perbaiki papan jembatan untuk menemukan barang rahasia.\"",
          riddle =
            RiddleChallenge(
              question = "Aku selalu berlari, tidak pernah lelah, dan tidak punya kaki. Siapakah aku?",
              hint = "Lihat sesuatu yang sedang mengalir tepat di bawah jembatan kayu ini!",
              options = listOf("Sungai", "Angin", "Bayangan"),
              correctIndex = 0,
              successText = "Tepat sekali! Peri air tersenyum dan memberimu hadiah.",
              wrongHintText = "Belum tepat, coba perhatikan air yang terus mengalir di bawahmu!",
            ),
          choices =
            listOf(
              StoryChoice(
                id = "forest_stream_riddle_win",
                label = "Lanjutkan Setelah Teka-Teki",
                badge = "Hadiah • +20 XP",
                xpReward = 20,
                coinReward = 15,
                nextNodeId = "forest_guardian",
                resultMessage = "Peri air memberimu 15 koin emas dan membuka jalan menuju padang rumput!",
                journalTitle = "Menjawab Teka-Teki Peri Air",
                journalSummary = "Berhasil memecahkan teka-teki Sungai Berkilau di Hutan Bisikan.",
              ),
              StoryChoice(
                id = "forest_stream_bridge",
                label = "Perbaiki Papan Jembatan",
                badge = "Dapat Kunci • -2 Energi",
                energyCost = 2,
                xpReward = 20,
                coinReward = 10,
                nextNodeId = "forest_guardian",
                resultMessage = "Di balik papan kayu kamu menemukan Kunci Lumut berkilau!",
                itemReward =
                  ItemReward(
                    itemKey = "key_moss",
                    name = "Kunci Lumut",
                    description = "Kunci kuno berukir daun dari jembatan Sungai Berkilau.",
                    category = "key",
                    quantity = 1L,
                    iconKey = "key",
                  ),
              ),
            ),
        ),
        StoryNode(
          id = "forest_guardian",
          regionId = "forest",
          title = "Padang Rusa Lumut",
          speaker = "Rusa Penjaga Rimba",
          pipTip =
            "Tips Pemula: Dalam latihan pertemuan, kamu bisa 'Serang Lembut' atau pilih 'Sapa Ramah' untuk menang tanpa bertarung!",
          narrative =
            "Seekor Rusa bertanduk lumut berdiri anggun di tengah padang bunga. Ia ingin menguji kesiapanmu sebelum memasuki altar hutan.\n\n\"Jangan khawatir, ini hanya latihan persahabatan untuk petualang pemula!\"",
          encounter =
            FriendlyEncounter(
              enemyName = "Rusa Lumut",
              enemyEmoji = "🦌",
              maxHp = 18,
              enemyDamage = 2,
              introLine = "Rusa Lumut siap menguji ketangkasanmu dengan lembut!",
              peaceCostEnergy = 2,
              victoryText = "Rusa Lumut mengangguk bangga dan memberimu Ramuan Herbal!",
            ),
          choices =
            listOf(
              StoryChoice(
                id = "forest_guardian_clear",
                label = "Melangkah ke Altar Pohon Tua",
                badge = "Hadiah Duel • +30 XP",
                xpReward = 30,
                coinReward = 20,
                nextNodeId = "forest_shrine",
                resultMessage = "Rusa Lumut menghadiahkan Ramuan Herbal untuk perjalananmu!",
                itemReward =
                  ItemReward(
                    itemKey = "potion_herb",
                    name = "Ramuan Herbal",
                    description = "Memulihkan 18 HP saat kamu membutuhkannya.",
                    category = "potion",
                    quantity = 1L,
                    healAmount = 18L,
                    iconKey = "potion",
                  ),
                journalTitle = "Persahabatan dengan Rusa Lumut",
                journalSummary = "Menyelesaikan latihan pertama di padang Hutan Bisikan.",
              )
            ),
        ),
        StoryNode(
          id = "forest_shrine",
          regionId = "forest",
          title = "Altar Pohon Tua",
          speaker = "Pip si Burung Hantu",
          pipTip =
            "Tips Pemula: Menyelesaikan satu wilayah akan membuka wilayah berikutnya di Peta Wilayah!",
          narrative =
            "Kamu tiba di akar raksasa Pohon Penjaga. Cahaya keemasan memancar dari sebuah lentera tua di atas altar batu.\n\n\"Hebat! Kamu hampir menuntaskan Hutan Bisikan. Ambil Lentera Emas untuk menerangi Reruntuhan Kuno!\"",
          choices =
            listOf(
              StoryChoice(
                id = "forest_shrine_lantern",
                label = "Ambil Lentera Emas & Lanjut",
                badge = "Selesai Wilayah 1 • +35 XP",
                xpReward = 35,
                coinReward = 25,
                nextNodeId = "ruins_gate",
                completesRegionId = "forest",
                resultMessage = "Hutan Bisikan selesai! Lentera Emas kini menerangi jalan ke Reruntuhan Kuno.",
                itemReward =
                  ItemReward(
                    itemKey = "lantern_gold",
                    name = "Lentera Emas",
                    description = "Relik cahaya abadi yang menerangi lorong Reruntuhan Kuno.",
                    category = "relic",
                    quantity = 1L,
                    iconKey = "lantern",
                  ),
                journalTitle = "Penjelajah Hutan Bisikan",
                journalSummary = "Berhasil menamatkan Wilayah 1 dan memperoleh Lentera Emas.",
              ),
              StoryChoice(
                id = "forest_shrine_rest",
                label = "Istirahat Sebentar Lalu Lanjut",
                badge = "Pulih Penuh • +25 XP",
                hpDelta = 30,
                xpReward = 25,
                coinReward = 20,
                nextNodeId = "ruins_gate",
                completesRegionId = "forest",
                resultMessage = "Energi dan HP pulih segar! Perjalanan berlanjut ke Reruntuhan Kuno.",
                journalTitle = "Penjelajah Hutan Bisikan",
                journalSummary = "Beristirahat di Altar Pohon Tua sebelum menuju Reruntuhan Kuno.",
              ),
            ),
        ),

        // --- WILAYAH 2: RERUNTUHAN KUNO ---
        StoryNode(
          id = "ruins_gate",
          regionId = "ruins",
          title = "Gerbang Reruntuhan Kuno",
          speaker = "Pip si Burung Hantu",
          pipTip =
            "Tips: Kamu selalu bisa mengecek progres semua wilayah lewat tab Peta di bagian bawah!",
          narrative =
            "Lengkungan batu berusia ratusan tahun berdiri megah di balik tirai sulur hijau. Simbol-simbol biru mulai menyala menyambut kedatanganmu.\n\n\"Wah, suasana di sini penuh misteri! Kita masuk lewat lorong utama atau periksa ukiran dinding dulu?\"",
          choices =
            listOf(
              StoryChoice(
                id = "ruins_gate_enter",
                label = "Masuk ke Aula Rune Bercahaya",
                badge = "Aman • +15 XP",
                xpReward = 15,
                coinReward = 12,
                nextNodeId = "ruins_hall",
                resultMessage = "Lentera memantulkan cahaya indah di sepanjang lorong batu.",
              ),
              StoryChoice(
                id = "ruins_gate_inspect",
                label = "Teliti Ukiran Lumut Dinding",
                badge = "Temuan • -2 Energi",
                energyCost = 2,
                xpReward = 22,
                coinReward = 18,
                nextNodeId = "ruins_hall",
                resultMessage = "Kamu menemukan koin kuno dan Gulungan Peta di celah batu!",
                itemReward =
                  ItemReward(
                    itemKey = "scroll_wisdom",
                    name = "Gulungan Peta Kuno",
                    description = "Catatan para penjelajah terdahulu berisi simbol rahasia.",
                    category = "relic",
                    quantity = 1L,
                    iconKey = "scroll",
                  ),
              ),
            ),
        ),
        StoryNode(
          id = "ruins_hall",
          regionId = "ruins",
          title = "Aula Rune Bercahaya",
          speaker = "Prasasti Cahaya",
          pipTip =
            "Tips: Jika kehabisan HP atau Energi, tekan tombol 'Kemah' di pojok atas atau gunakan camilan di Tas!",
          narrative =
            "Di tengah aula berdiri pedestal batu yang memancarkan suara lembut. Untuk membuka pintu dalam, sebuah teka-teki klasik muncul di udara.",
          riddle =
            RiddleChallenge(
              question = "Semakin banyak aku diambil, semakin besar diriku. Apakah aku?",
              hint = "Bayangkan kamu menggali tanah di halaman!",
              options = listOf("Lubang", "Batu", "Awan"),
              correctIndex = 0,
              successText = "Rune biru bersinar terang! Pintu batu bergeser terbuka.",
              wrongHintText = "Belum tepat! Coba pikirkan apa yang terjadi saat kita menggali tanah.",
            ),
          choices =
            listOf(
              StoryChoice(
                id = "ruins_hall_next",
                label = "Masuk ke Ruang Penjaga Batu",
                badge = "Hadiah Teka-Teki • +25 XP",
                xpReward = 25,
                coinReward = 20,
                nextNodeId = "ruins_golem",
                resultMessage = "Kamu memperoleh Kunci Batu dan melangkah masuk ke ruang penjaga!",
                itemReward =
                  ItemReward(
                    itemKey = "key_stone",
                    name = "Kunci Batu Rune",
                    description = "Kunci bercahaya pirus dari Aula Rune.",
                    category = "key",
                    quantity = 1L,
                    iconKey = "key",
                  ),
              )
            ),
        ),
        StoryNode(
          id = "ruins_golem",
          regionId = "ruins",
          title = "Penjaga Golem Mini",
          speaker = "Golem Batu Mungil",
          pipTip =
            "Tips: Kalau HP menipis, kamu bisa langsung menggunakan Ramuan Herbal di tengah duel!",
          narrative =
            "Sebuah patung batu kecil setinggi lutut bergerak hidup! Matanya berkedip seperti lampu biru.\n\n\"Krik-kruk! Buktikan kamu adalah sahabat para penjelajah!\"",
          encounter =
            FriendlyEncounter(
              enemyName = "Golem Mini",
              enemyEmoji = "🗿",
              maxHp = 24,
              enemyDamage = 3,
              introLine = "Golem Mini bersiap menguji ketangguhanmu!",
              peaceCostEnergy = 2,
              victoryText = "Golem Mini bertepuk tangan dan menyerahkan Jimat Burung Hantu!",
            ),
          choices =
            listOf(
              StoryChoice(
                id = "ruins_golem_clear",
                label = "Menuju Ruang Mahkota Kuno",
                badge = "Hadiah Duel • +40 XP",
                xpReward = 40,
                coinReward = 30,
                nextNodeId = "ruins_sanctum",
                resultMessage = "Jimat Burung Hantu kini tersimpan rapi di tas ranselmu!",
                itemReward =
                  ItemReward(
                    itemKey = "amulet_owl",
                    name = "Jimat Burung Hantu",
                    description = "Simbol kebijaksanaan pelindung para petualang.",
                    category = "relic",
                    quantity = 1L,
                    iconKey = "amulet",
                  ),
                journalTitle = "Bersahabat dengan Golem Mini",
                journalSummary = "Berhasil melewati ujian Golem Mini dan meraih Jimat Burung Hantu.",
              )
            ),
        ),
        StoryNode(
          id = "ruins_sanctum",
          regionId = "ruins",
          title = "Ruang Mahkota Kuno",
          speaker = "Pip si Burung Hantu",
          pipTip = "Hebat! Satu wilayah lagi menuju puncak tertinggi Loka Petualang!",
          narrative =
            "Tangga spiral membawa kita keluar dari reruntuhan menuju lembah awan perak. Di kejauhan, Puncak Kristal berkilau memanggil.\n\n\"Luar biasa! Reruntuhan Kuno telah kamu taklukkan. Siap mendaki Puncak Kristal?\"",
          choices =
            listOf(
              StoryChoice(
                id = "ruins_sanctum_advance",
                label = "Mendaki ke Puncak Kristal",
                badge = "Selesai Wilayah 2 • +45 XP",
                xpReward = 45,
                coinReward = 35,
                nextNodeId = "summit_trail",
                completesRegionId = "ruins",
                resultMessage = "Reruntuhan Kuno tuntas! Jalur Puncak Kristal kini terbuka lebar.",
                journalTitle = "Penakluk Reruntuhan Kuno",
                journalSummary = "Menuntaskan semua misteri di Reruntuhan Kuno.",
              )
            ),
        ),

        // --- WILAYAH 3: PUNCAK KRISTAL ---
        StoryNode(
          id = "summit_trail",
          regionId = "summit",
          title = "Jalur Awan Perak",
          speaker = "Pip si Burung Hantu",
          pipTip = "Tips: Api unggun di puncak memulihkan tubuhmu sebelum tantangan terakhir!",
          narrative =
            "Kabut lembut melayang di bawah kaki kita. Di tepi tebing terdapat perkemahan hangat dengan api unggun yang menyala tenang.\n\n\"Udara pegunungan terasa segar sekali. Mau beristirahat sebentar atau berburu serpihan kristal?\"",
          choices =
            listOf(
              StoryChoice(
                id = "summit_trail_camp",
                label = "Hangatkan Diri di Api Unggun",
                badge = "Pulih HP & Energi • +15 XP",
                hpDelta = 15,
                xpReward = 15,
                coinReward = 10,
                nextNodeId = "summit_bridge",
                resultMessage = "Tubuhmu terasa hangat dan bertenaga kembali!",
              ),
              StoryChoice(
                id = "summit_trail_crystal",
                label = "Kumpulkan Serpihan Kristal",
                badge = "Relik • -2 Energi",
                energyCost = 2,
                xpReward = 25,
                coinReward = 20,
                nextNodeId = "summit_bridge",
                resultMessage = "Kamu menemukan Serpihan Kristal yang berkilau indah!",
                itemReward =
                  ItemReward(
                    itemKey = "crystal_shard",
                    name = "Serpihan Kristal",
                    description = "Kristal ungu berkilau dari lereng Puncak Kristal.",
                    category = "relic",
                    quantity = 1L,
                    iconKey = "crystal",
                  ),
              ),
            ),
        ),
        StoryNode(
          id = "summit_bridge",
          regionId = "summit",
          title = "Jembatan Cahaya Bintang",
          speaker = "Penjaga Bintang",
          pipTip = "Teka-teki terakhir sebelum puncak! Baca petunjuk jika butuh bantuan.",
          narrative =
            "Jembatan kristal terbentang menuju puncak menara. Sebuah suara lembut menguji pengetahuan penjelajahmu.",
          riddle =
            RiddleChallenge(
              question =
                "Aku punya kota tanpa rumah, hutan tanpa pohon, dan lautan tanpa air. Apakah aku?",
              hint = "Alat yang selalu dibawa petualang untuk melihat wilayah dan jalur!",
              options = listOf("Peta", "Mimpi", "Kompas"),
              correctIndex = 0,
              successText = "Jawabannya adalah Peta! Jembatan kristal menyala terang.",
              wrongHintText = "Belum tepat! Coba pikirkan benda kertas yang menunjukkan kota dan lautan.",
            ),
          choices =
            listOf(
              StoryChoice(
                id = "summit_bridge_next",
                label = "Seberangi Menuju Puncak",
                badge = "Hadiah • +30 XP",
                xpReward = 30,
                coinReward = 25,
                nextNodeId = "summit_dragon",
                resultMessage = "Kamu menyeberangi jembatan bintang dengan penuh percaya diri!",
              )
            ),
        ),
        StoryNode(
          id = "summit_dragon",
          regionId = "summit",
          title = "Naga Awan Mungil",
          speaker = "Naga Awan",
          pipTip = "Naga ini ramah kok! Gunakan Ramuan atau Negosiasi jika energimu cukup.",
          narrative =
            "Seekor naga mungil berwarna biru pastel melayang riang di sekitar puncak kristal.\n\n\"Groar kecil! Tunjukkan semangat petualangmu sebelum menyalakan Suar Harapan!\"",
          encounter =
            FriendlyEncounter(
              enemyName = "Naga Awan",
              enemyEmoji = "🐉",
              maxHp = 30,
              enemyDamage = 4,
              introLine = "Naga Awan menari di udara untuk ujian terakhir!",
              peaceCostEnergy = 3,
              victoryText = "Naga Awan tersenyum dan memberimu Lencana Petualang Utama!",
            ),
          choices =
            listOf(
              StoryChoice(
                id = "summit_dragon_win",
                label = "Masuk ke Menara Suar Kristal",
                badge = "Hadiah Bos • +60 XP",
                xpReward = 60,
                coinReward = 50,
                nextNodeId = "summit_beacon",
                resultMessage = "Kamu meraih Lencana Petualang Utama yang berkilau!",
                itemReward =
                  ItemReward(
                    itemKey = "crown_explorer",
                    name = "Lencana Petualang Utama",
                    description = "Penghargaan tertinggi bagi penjelajah Loka Petualang.",
                    category = "relic",
                    quantity = 1L,
                    iconKey = "crown",
                  ),
                journalTitle = "Menaklukkan Ujian Naga Awan",
                journalSummary = "Mengatasi ujian terakhir di Puncak Kristal dengan gemilang.",
              )
            ),
        ),
        StoryNode(
          id = "summit_beacon",
          regionId = "summit",
          title = "Menara Suar Kristal",
          speaker = "Pip si Burung Hantu",
          pipTip =
            "Selamat! Kamu sudah menuntaskan seluruh cerita utama. Kamu tetap bisa menjelajah ulang lewat tab Peta!",
          narrative =
            "Saat Lentera Emas didekatkan ke kristal utama, cahaya hangat memancar menerangi seluruh langit senja — dari Hutan Bisikan hingga Reruntuhan Kuno.\n\n\"Selamat, Petualang Sejati! Kisahmu kini tercatat abadi di Loka Petualang!\"",
          choices =
            listOf(
              StoryChoice(
                id = "summit_beacon_finish",
                label = "Nyalakan Suar Harapan!",
                badge = "Tamat Wilayah 3 • +80 XP",
                xpReward = 80,
                coinReward = 75,
                nextNodeId = "summit_beacon",
                completesRegionId = "summit",
                resultMessage = "Suar Harapan menyala! Seluruh Loka Petualang bersorak merayakan kemenanganmu!",
                journalTitle = "Cahaya Suar Puncak Kristal",
                journalSummary = "Menyalakan Suar Harapan dan menuntaskan petualangan Loka Petualang!",
              ),
              StoryChoice(
                id = "summit_beacon_return",
                label = "Kembali ke Hutan Bisikan",
                badge = "Eksplorasi Bebas",
                xpReward = 10,
                coinReward = 5,
                nextNodeId = "forest_start",
                resultMessage = "Kembali ke Gerbang Hutan Bisikan untuk eksplorasi bebas.",
              ),
            ),
        ),
      )
      .associateBy { it.id }

  fun allNodesMap(): Map<String, StoryNode> = nodes

  fun getRegion(regionId: String): RegionInfo =
    regions.find { it.id == regionId } ?: regions.first()

  fun getNode(nodeId: String): StoryNode =
    nodes[nodeId] ?: nodes.getValue("forest_start")

  fun isRegionUnlocked(regionId: String, completedRegionIds: List<String>): Boolean {
    return when (regionId) {
      "forest" -> true
      "ruins" -> completedRegionIds.contains("forest")
      "summit" -> completedRegionIds.contains("ruins")
      else -> false
    }
  }

  fun xpRequiredForNextLevel(level: Long): Long = level * 50L
}
