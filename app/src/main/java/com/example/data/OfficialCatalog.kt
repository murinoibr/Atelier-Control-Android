package com.example.data

import com.example.data.model.Product

object OfficialCatalog {

    const val CATALOG_NUMBER = "1023"
    const val ARTIST_NAME = "Jonas Lemes"
    const val CATALOG_TITLE = "Tabela de Obras — Jonas Lemes"
    const val CATALOG_SUBTITLE = "Catálogo Oficial do Acervo de Pinturas & Gravuras"

    val featuredArtwork = Product(
        name = "Ipê na Serra",
        price = 23000.00,
        category = "Pinturas / Obras Originais",
        code = "Destaque",
        dimensions = "100 × 160 cm",
        year = "2024",
        status = "VENDIDO",
        technique = "Pintura Original (Óleo/Acrílica sobre tela)",
        notes = "★ Obra em Destaque (Anotação Superior)",
        artist = ARTIST_NAME
    )

    fun getOfficialCatalog(): List<Product> = listOf(
        // ★ OBRA EM DESTAQUE (ANOTAÇÃO SUPERIOR)
        featuredArtwork,

        // PINTURAS / OBRAS ORIGINAIS (28 OBRAS DA TABELA)
        Product(
            name = "[Obra s/ título registrado]",
            price = 3100.00,
            category = "Pinturas / Obras Originais",
            code = "S/N",
            dimensions = "37 × 53 cm",
            year = "2024",
            status = "VENDIDO",
            technique = "Pintura Original",
            notes = "Sem título registrado",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Feira",
            price = 3100.00,
            category = "Pinturas / Obras Originais",
            code = "956",
            dimensions = "42 × 32 cm",
            year = "2024",
            status = "DISPONÍVEL",
            technique = "Pintura Original",
            notes = "",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Feira",
            price = 3600.00,
            category = "Pinturas / Obras Originais",
            code = "957",
            dimensions = "41 × 54 cm",
            year = "2024",
            status = "DISPONÍVEL",
            technique = "Pintura Original",
            notes = "",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Feira",
            price = 2300.00,
            category = "Pinturas / Obras Originais",
            code = "958",
            dimensions = "24 × 18 cm",
            year = "2024",
            status = "DISPONÍVEL",
            technique = "Pintura Original",
            notes = "",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Ipê",
            price = 2300.00,
            category = "Pinturas / Obras Originais",
            code = "960",
            dimensions = "26 × 20 cm",
            year = "2024",
            status = "DISPONÍVEL",
            technique = "Pintura Original",
            notes = "",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Ipês",
            price = 2300.00,
            category = "Pinturas / Obras Originais",
            code = "961",
            dimensions = "25 × 19 cm",
            year = "2024",
            status = "DISPONÍVEL",
            technique = "Pintura Original",
            notes = "",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Ipês",
            price = 2300.00,
            category = "Pinturas / Obras Originais",
            code = "963",
            dimensions = "27 × 19 cm",
            year = "2024",
            status = "DISPONÍVEL",
            technique = "Pintura Original",
            notes = "",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Ipês",
            price = 2300.00,
            category = "Pinturas / Obras Originais",
            code = "964",
            dimensions = "24 × 19 cm",
            year = "2024",
            status = "DISPONÍVEL",
            technique = "Pintura Original",
            notes = "",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Ipês",
            price = 2300.00,
            category = "Pinturas / Obras Originais",
            code = "965",
            dimensions = "23 × 18 cm",
            year = "2024",
            status = "DISPONÍVEL",
            technique = "Pintura Original",
            notes = "",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Ipês",
            price = 2300.00,
            category = "Pinturas / Obras Originais",
            code = "966",
            dimensions = "24 × 18 cm",
            year = "2024",
            status = "DISPONÍVEL",
            technique = "Pintura Original",
            notes = "",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Ipês",
            price = 2300.00,
            category = "Pinturas / Obras Originais",
            code = "967",
            dimensions = "24 × 18 cm",
            year = "2024",
            status = "DISPONÍVEL",
            technique = "Pintura Original",
            notes = "",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Ipês",
            price = 2300.00,
            category = "Pinturas / Obras Originais",
            code = "968",
            dimensions = "24 × 18 cm",
            year = "2024",
            status = "VENDIDO",
            technique = "Pintura Original",
            notes = "",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Ipês",
            price = 2300.00,
            category = "Pinturas / Obras Originais",
            code = "969",
            dimensions = "27 × 20 cm",
            year = "2024",
            status = "DISPONÍVEL",
            technique = "Pintura Original",
            notes = "",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Ipês em Minas",
            price = 13700.00,
            category = "Pinturas / Obras Originais",
            code = "970",
            dimensions = "109 × 74 cm",
            year = "2024",
            status = "DISPONÍVEL",
            technique = "Pintura Original",
            notes = "",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Ipês em Minas",
            price = 13700.00,
            category = "Pinturas / Obras Originais",
            code = "971",
            dimensions = "109 × 74 cm",
            year = "2024",
            status = "VENDIDO",
            technique = "Pintura Original",
            notes = "",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Ipê no Vale",
            price = 7400.00,
            category = "Pinturas / Obras Originais",
            code = "972",
            dimensions = "50 × 80 cm",
            year = "2024",
            status = "VENDIDO",
            technique = "Pintura Original",
            notes = "",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Cavalgada",
            price = 25000.00,
            category = "Pinturas / Obras Originais",
            code = "973",
            dimensions = "98 × 172 cm",
            year = "2024",
            status = "VENDIDO",
            technique = "Pintura Original",
            notes = "",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Flamboyant",
            price = 2300.00,
            category = "Pinturas / Obras Originais",
            code = "974",
            dimensions = "25 × 20 cm",
            year = "2025",
            status = "DISPONÍVEL",
            technique = "Pintura Original",
            notes = "",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Flamboyant",
            price = 2300.00,
            category = "Pinturas / Obras Originais",
            code = "975",
            dimensions = "25 × 20 cm",
            year = "2025",
            status = "DISPONÍVEL",
            technique = "Pintura Original",
            notes = "",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Flamboyant",
            price = 2500.00,
            category = "Pinturas / Obras Originais",
            code = "976",
            dimensions = "25 × 35 cm",
            year = "2025",
            status = "DISPONÍVEL",
            technique = "Pintura Original",
            notes = "",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Flamboyant",
            price = 2500.00,
            category = "Pinturas / Obras Originais",
            code = "977",
            dimensions = "25 × 35 cm",
            year = "2025",
            status = "DISPONÍVEL",
            technique = "Pintura Original",
            notes = "",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Flamboyant",
            price = 3400.00,
            category = "Pinturas / Obras Originais",
            code = "978",
            dimensions = "38 × 57 cm",
            year = "2025",
            status = "VENDIDO",
            technique = "Pintura Original",
            notes = "",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Praça de Flamboyants",
            price = 2800.00,
            category = "Pinturas / Obras Originais",
            code = "984",
            dimensions = "32 × 45 cm",
            year = "2025",
            status = "DISPONÍVEL",
            technique = "Pintura Original",
            notes = "",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Praça 21 de Abril",
            price = 8000.00,
            category = "Pinturas / Obras Originais",
            code = "985",
            dimensions = "50 × 89 cm",
            year = "2025",
            status = "DISPONÍVEL",
            technique = "Pintura Original",
            notes = "",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Fornalhas",
            price = 2800.00,
            category = "Pinturas / Obras Originais",
            code = "986",
            dimensions = "32 × 45 cm",
            year = "2025",
            status = "VENDIDO",
            technique = "Pintura Original",
            notes = "",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Alto da Pedra – São Lourenço",
            price = 8000.00,
            category = "Pinturas / Obras Originais",
            code = "996",
            dimensions = "50 × 89 cm",
            year = "2025",
            status = "VENDIDO",
            technique = "Pintura Original",
            notes = "",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Ipês",
            price = 2300.00,
            category = "Pinturas / Obras Originais",
            code = "962",
            dimensions = "26 × 20 cm",
            year = "2024",
            status = "VENDIDO",
            technique = "Pintura Original",
            notes = "Adição manual",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Contemplação",
            price = 20000.00,
            category = "Pinturas / Obras Originais",
            code = "1000",
            dimensions = "90 × 147 cm",
            year = "2025",
            status = "DISPONÍVEL",
            technique = "Pintura Original",
            notes = "Adição manual",
            artist = ARTIST_NAME
        ),

        // COLEÇÃO DE GRAVURAS - GRAVURAS NO PAPEL (PAPEL ESPECIAL)
        Product(
            name = "Galinha, Arado e Ipê na Serra",
            price = 130.00,
            category = "Gravuras no Papel",
            code = "GRAV-P-130",
            dimensions = "Papel Especial",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura sobre Papel Especial",
            notes = "Série: Galinha, Arado e Ipê na Serra",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Ipê com Carroça e Ipê com Porteira",
            price = 160.00,
            category = "Gravuras no Papel",
            code = "GRAV-P-160",
            dimensions = "Papel Especial",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura sobre Papel Especial",
            notes = "Série: Ipê com Carroça e Ipê com Porteira",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Parada Ramon, Vichy, Ipês em S.Lço, Neblina e Balneário",
            price = 190.00,
            category = "Gravuras no Papel",
            code = "GRAV-P-190",
            dimensions = "Papel Especial",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura sobre Papel Especial",
            notes = "Série: Parada Ramon, Vichy, Ipês em São Lourenço, Neblina e Balneário",
            artist = ARTIST_NAME
        ),

        // GRAVURAS NO PAPEL - OBRAS INDIVIDUAIS
        Product(
            name = "Galinha (Papel Especial)",
            price = 130.00,
            category = "Gravuras no Papel",
            code = "GP-01",
            dimensions = "Papel Especial",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura sobre Papel Especial",
            notes = "Gravura individual",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Arado (Papel Especial)",
            price = 130.00,
            category = "Gravuras no Papel",
            code = "GP-02",
            dimensions = "Papel Especial",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura sobre Papel Especial",
            notes = "Gravura individual",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Ipê na Serra (Papel Especial)",
            price = 130.00,
            category = "Gravuras no Papel",
            code = "GP-03",
            dimensions = "Papel Especial",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura sobre Papel Especial",
            notes = "Gravura individual",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Ipê com Carroça (Papel Especial)",
            price = 160.00,
            category = "Gravuras no Papel",
            code = "GP-04",
            dimensions = "Papel Especial",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura sobre Papel Especial",
            notes = "Gravura individual",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Ipê com Porteira (Papel Especial)",
            price = 160.00,
            category = "Gravuras no Papel",
            code = "GP-05",
            dimensions = "Papel Especial",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura sobre Papel Especial",
            notes = "Gravura individual",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Parada Ramon (Papel Especial)",
            price = 190.00,
            category = "Gravuras no Papel",
            code = "GP-06",
            dimensions = "Papel Especial",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura sobre Papel Especial",
            notes = "Gravura individual",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Vichy (Papel Especial)",
            price = 190.00,
            category = "Gravuras no Papel",
            code = "GP-07",
            dimensions = "Papel Especial",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura sobre Papel Especial",
            notes = "Gravura individual",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Ipês em S. Lço (Papel Especial)",
            price = 190.00,
            category = "Gravuras no Papel",
            code = "GP-08",
            dimensions = "Papel Especial",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura sobre Papel Especial",
            notes = "Gravura individual",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Neblina (Papel Especial)",
            price = 190.00,
            category = "Gravuras no Papel",
            code = "GP-09",
            dimensions = "Papel Especial",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura sobre Papel Especial",
            notes = "Gravura individual",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Balneário (Papel Especial)",
            price = 190.00,
            category = "Gravuras no Papel",
            code = "GP-10",
            dimensions = "Papel Especial",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura sobre Papel Especial",
            notes = "Gravura individual",
            artist = ARTIST_NAME
        ),

        // COLEÇÃO DE GRAVURAS - GRAVURAS COM MOLDURA
        Product(
            name = "Ipê na Serra, Arado, Galinha",
            price = 300.00,
            category = "Gravuras com Moldura",
            code = "GRAV-M-300",
            dimensions = "Com Moldura",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura com Moldura",
            notes = "Série: Ipê na Serra, Arado, Galinha",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Ipê com Carroça e Ipê com Porteira",
            price = 400.00,
            category = "Gravuras com Moldura",
            code = "GRAV-M-400",
            dimensions = "Com Moldura",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura com Moldura",
            notes = "Série: Ipê com Carroça e Ipê com Porteira",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Parada Ramon, Vichy, Ipês em S.Lço, Neblina e Balneário",
            price = 500.00,
            category = "Gravuras com Moldura",
            code = "GRAV-M-500",
            dimensions = "Com Moldura",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura com Moldura",
            notes = "Série: Parada Ramon, Vichy, Ipês em São Lourenço, Neblina e Balneário",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Neblina média",
            price = 350.00,
            category = "Gravuras com Moldura",
            code = "GRAV-M-350",
            dimensions = "Com Moldura",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura com Moldura",
            notes = "Anotação manual",
            artist = ARTIST_NAME
        ),

        // GRAVURAS COM MOLDURA - OBRAS INDIVIDUAIS
        Product(
            name = "Ipê na Serra (Com Moldura)",
            price = 300.00,
            category = "Gravuras com Moldura",
            code = "GM-01",
            dimensions = "Com Moldura",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura com Moldura",
            notes = "Gravura com moldura",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Arado (Com Moldura)",
            price = 300.00,
            category = "Gravuras com Moldura",
            code = "GM-02",
            dimensions = "Com Moldura",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura com Moldura",
            notes = "Gravura com moldura",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Galinha (Com Moldura)",
            price = 300.00,
            category = "Gravuras com Moldura",
            code = "GM-03",
            dimensions = "Com Moldura",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura com Moldura",
            notes = "Gravura com moldura",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Ipê com Carroça (Com Moldura)",
            price = 400.00,
            category = "Gravuras com Moldura",
            code = "GM-04",
            dimensions = "Com Moldura",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura com Moldura",
            notes = "Gravura com moldura",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Ipê com Porteira (Com Moldura)",
            price = 400.00,
            category = "Gravuras com Moldura",
            code = "GM-05",
            dimensions = "Com Moldura",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura com Moldura",
            notes = "Gravura com moldura",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Parada Ramon (Com Moldura)",
            price = 500.00,
            category = "Gravuras com Moldura",
            code = "GM-06",
            dimensions = "Com Moldura",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura com Moldura",
            notes = "Gravura com moldura",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Vichy (Com Moldura)",
            price = 500.00,
            category = "Gravuras com Moldura",
            code = "GM-07",
            dimensions = "Com Moldura",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura com Moldura",
            notes = "Gravura com moldura",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Ipês em S. Lço (Com Moldura)",
            price = 500.00,
            category = "Gravuras com Moldura",
            code = "GM-08",
            dimensions = "Com Moldura",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura com Moldura",
            notes = "Gravura com moldura",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Neblina (Com Moldura)",
            price = 500.00,
            category = "Gravuras com Moldura",
            code = "GM-09",
            dimensions = "Com Moldura",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura com Moldura",
            notes = "Gravura com moldura",
            artist = ARTIST_NAME
        ),
        Product(
            name = "Balneário (Com Moldura)",
            price = 500.00,
            category = "Gravuras com Moldura",
            code = "GM-10",
            dimensions = "Com Moldura",
            year = "2024-2025",
            status = "DISPONÍVEL",
            technique = "Gravura com Moldura",
            notes = "Gravura com moldura",
            artist = ARTIST_NAME
        )
    )
}
