package com.zhengyang.redbook.ui.home

object HomeMockData {

    data class FollowingUser(
        val id: String,
        val name: String,
        val subtitle: String,
        val avatarColorHex: String,
        val badge: String? = null
    )

    enum class ContentBucket(val usesWaterfall: Boolean) {
        RECOMMEND(true),
        RED(true),
        LIVE(false),
        DRAMA(false),
        TIPS(false),
        OUTFIT(true),
        FOOD(false),
        TRAVEL(false)
    }

    enum class DiscoverCategory(val title: String, val bucket: ContentBucket) {
        RECOMMEND("\u63a8\u8350", ContentBucket.RECOMMEND),
        RED("RED", ContentBucket.RED),
        LIVE("\u76f4\u64ad", ContentBucket.LIVE),
        DRAMA("\u77ed\u5267", ContentBucket.DRAMA),
        TIPS("\u7ecf\u9a8c", ContentBucket.TIPS),
        OUTFIT("\u7a7f\u642d", ContentBucket.OUTFIT),
        FOOD("\u7f8e\u98df", ContentBucket.FOOD),
        EMOTION("\u60c5\u611f", ContentBucket.DRAMA),
        TRAVEL("\u65c5\u884c", ContentBucket.TRAVEL),
        PHOTO("\u6444\u5f71", ContentBucket.TRAVEL),
        CAR("\u6c7d\u8f66", ContentBucket.LIVE),
        DANCE("\u821e\u8e48", ContentBucket.DRAMA),
        AVATAR("\u5934\u50cf", ContentBucket.TIPS),
        WALLPAPER("\u58c1\u7eb8", ContentBucket.TIPS),
        FUNNY("\u641e\u7b11", ContentBucket.DRAMA),
        FITNESS("\u5065\u8eab\u5851\u578b", ContentBucket.OUTFIT),
        HOME("\u5bb6\u5c45", ContentBucket.TIPS),
        GROOMING("\u7537\u58eb\u7406\u5bb9", ContentBucket.OUTFIT),
        CAREER("\u804c\u573a", ContentBucket.TIPS),
        MUSIC("\u97f3\u4e50", ContentBucket.LIVE),
        RENOVATION("\u5bb6\u88c5", ContentBucket.TIPS),
        TECH("\u79d1\u6280\u6570\u7801", ContentBucket.RED),
        FILM("\u5f71\u89c6", ContentBucket.DRAMA),
        PAINTING("\u7ed8\u753b", ContentBucket.TIPS),
        READING("\u8bfb\u4e66", ContentBucket.TIPS),
        STUDY("\u5b66\u4e60", ContentBucket.TIPS),
        SNEAKERS("\u6f6e\u978b", ContentBucket.OUTFIT),
        SCIENCE("\u79d1\u5b66\u79d1\u666e", ContentBucket.TIPS),
        GAME("\u6e38\u620f", ContentBucket.DRAMA),
        ART("\u827a\u672f", ContentBucket.TIPS),
        WEDDING("\u5a5a\u793c", ContentBucket.OUTFIT),
        ANIME("\u52a8\u6f2b", ContentBucket.DRAMA),
        CRAFT("\u624b\u5de5", ContentBucket.TIPS),
        FAT_LOSS("\u51cf\u8102", ContentBucket.OUTFIT),
        MOTOR("\u673a\u8f66", ContentBucket.LIVE),
        SPORTS("\u4f53\u80b2", ContentBucket.LIVE),
        PET("\u840c\u5ba0", ContentBucket.TIPS),
        CELEBRITY("\u660e\u661f", ContentBucket.RED),
        CULTURE("\u6587\u5316", ContentBucket.TIPS),
        SOCIAL("\u793e\u79d1", ContentBucket.TIPS),
        OUTDOOR("\u6237\u5916", ContentBucket.TRAVEL),
        MOM_BABY("\u6bcd\u5a74", ContentBucket.TIPS),
        SKINCARE("\u62a4\u80a4", ContentBucket.OUTFIT),
        PSYCHOLOGY("\u5fc3\u7406", ContentBucket.TIPS),
        ESPORTS("\u7ade\u6280\u4f53\u80b2", ContentBucket.DRAMA),
        VARIETY("\u7efc\u827a", ContentBucket.DRAMA),
        TOYS("\u6f6e\u73a9\u624b\u529e", ContentBucket.RED),
        CAMPUS("\u6821\u56ed\u751f\u6d3b", ContentBucket.TIPS),
        CAMPING("\u9732\u8425", ContentBucket.TRAVEL);

        val usesWaterfall: Boolean
            get() = bucket.usesWaterfall
    }

    fun itemsFor(category: DiscoverCategory): List<HomeCardItem> = when (category.bucket) {
        ContentBucket.RECOMMEND -> recommendItems()
        ContentBucket.RED -> redItems()
        ContentBucket.LIVE -> liveItems()
        ContentBucket.DRAMA -> dramaItems()
        ContentBucket.TIPS -> tipsItems()
        ContentBucket.OUTFIT -> outfitItems()
        ContentBucket.FOOD -> foodItems()
        ContentBucket.TRAVEL -> travelItems()
    }

    fun suggestedFollowingUsers() = listOf(
        FollowingUser("u1", "Roo_若", "热门作者 74.1万粉丝", "#9EBF7B"),
        FollowingUser("u2", "下铺小涵", "热门作者 346.5万粉丝", "#B98D82"),
        FollowingUser("u3", "AriaAndBrandon", "vlog内容创作者", "#3C4C58", "●"),
        FollowingUser("u4", "清平乐", "美妆内容热门作者", "#D9C4A6"),
        FollowingUser("u5", "这英", "生活记录内容热门作者", "#6C5950"),
        FollowingUser("u6", "__Kiraaa", "美妆内容热门作者", "#A0B4A5"),
        FollowingUser("u7", "IC实验室", "美食内容热门作者", "#F0C44B"),
        FollowingUser("u8", "张予曦 yci", "演员", "#A37D87", "●")
    )

    fun followingFeedItems() = listOf(
        card("f1", "好好吃主食的我，气血充盈的掉了20斤", "欧阳食余菲", "2518", "纯经验分享", "不挨饿无高强度运动", 262, "#C7B66B", "#526B3A", "#F3D8A3"),
        card("f2", "夏天喝姜枣茶，出现什么反应才是正常的呢？", "欧阳食余菲", "246", "姜枣茶避雷", "千万别跟风喝姜枣茶", 292, "#F0B06E", "#8A4E1D", "#F1C39E"),
        card("f3", "阳气足了，指甲是很快能看出来的", "欧阳食余菲", "681", "欧阳新专辑", "九大体质系列05", 248, "#B56521", "#5A2B10", "#E4B083"),
        card("f4", "10分钟完整跟练 无加速", "二歪瑜伽", "982", "欧阳商场探腹", "产后居家恢复", 286, "#BC988E", "#6A4137", "#E5C0B7")
    )

    private fun recommendItems() = listOf(
        card("1", "\u5c0f\u7ea2\u4e66\u6210 2026 \u5e74\u7f8e\u52a0\u58a8\u4e16\u754c\u676f\u6301\u6743\u8f6c\u64ad\u5546", "\u542f\u961f\u957f", "10\u4e07+", "\u70ed\u70b9", "\u770b\u4e16\u754c\u676f", 246, "#D8082B", "#6D000F", "#FF8A9F"),
        card("2", "\u89c1\u82b1\u5982\u9762\uff0c\u4eca\u5929\u7684\u6625\u5929\u521a\u521a\u597d", "\u5468\u5468 Kiko", "355", "\u7a7f\u642d", "\u89c1\u82b1\u5982\u9762", 292, "#4B8D5C", "#253A24", "#FFDCC8"),
        card("3", "\u6709\u59d0\u8ff7\u59d0\u6ca1\u59d0\u91cd\u5f00\uff0c\u6ee8\u6c5f\u6563\u6b65\u51fa\u7247\u653b\u7565", "\u5c0f\u6811\u83ab", "2.4\u4e07+", "\u63a8\u8350", "\u6c5f\u8fb9\u98ce", 218, "#7BA7F7", "#355AA1", "#FFE1C9"),
        card("4", "\u6c99\u96d5\u5bf9\u8bdd\u5408\u96c6\uff0c\u7761\u524d\u770b\u4e00\u6761\u5c31\u591f\u4e86", "\u9ebb\u660e\u65af\u5427", "6949", "\u804a\u5929", "\u54c8\u54c8\u54c8", 268, "#E8EEF9", "#8FA2C7", "#F6C66E"),
        card("5", "\u4eca\u5929\u505a\u4e2a\u4f4e\u9971\u548c\u5986\uff0c\u901a\u52e4\u4e5f\u80fd\u5f88\u7cbe\u795e", "\u5377\u5377\u5316\u5986\u95f4", "1.2\u4e07+", "\u5986\u5bb9", "\u4f4e\u9971\u548c", 238, "#D6B4A9", "#7E5851", "#F2D2C8"),
        card("6", "\u665a\u98ce\u5f88\u9002\u5408\u9a91\u8f66\uff0c\u9646\u5bb6\u5634\u591c\u666f\u771f\u7684\u7edd", "HaoTrip", "881", "\u65c5\u884c", "\u591c\u9a91", 302, "#335C89", "#111A2B", "#A5D1FF")
    )

    private fun redItems() = listOf(
        card("7", "RED \u65b0\u4eba\u5f00\u64ad\u7b2c\u4e00\u5468\uff0c\u8d26\u53f7\u8d77\u91cf\u8bb0\u5f55", "\u963f\u7d2b", "8.8\u4e07+", "RED", "\u65b0\u53f7\u8d77\u91cf", 236, "#D71F3B", "#6C091A", "#FFBBC7"),
        card("8", "\u7528 3 \u5957\u6a21\u677f\u505a\u51fa\u7edf\u4e00\u5c01\u9762\uff0c\u6548\u7387\u7ffb\u500d", "\u8bbe\u8ba1\u5175", "736", "\u6548\u7387", "\u5c01\u9762\u6a21\u677f", 278, "#F66A4C", "#6A2417", "#FFD4A1"),
        card("9", "\u4ece\u96f6\u5f00\u59cb\u517b\u6210\u4e3b\u9875\u8c03\u6027\uff0c\u522b\u518d\u4e71\u53d1\u4e86", "Jelly Studio", "4.1\u4e07+", "\u65b9\u6cd5", "\u4e3b\u9875\u8c03\u6027", 212, "#3C415C", "#16182A", "#D2D7F7"),
        card("10", "\u505a\u5185\u5bb9\u4e5f\u8981\u4f1a\u8bb2\u6545\u4e8b\uff0c\u7b2c\u4e00\u5c4f\u5c31\u5f97\u6293\u4eba", "Nana", "1920", "\u8868\u8fbe", "\u4f1a\u8bb2\u6545\u4e8b", 290, "#B94E8C", "#4D1D38", "#F7C1DD")
    )

    private fun liveItems() = listOf(
        card("11", "\u76f4\u64ad\u642d\u5efa\u6e05\u5355\uff0c\u706f\u5149\u673a\u4f4d\u548c\u80cc\u666f\u4e00\u6b21\u914d\u9f50", "\u76f4\u64ad\u5b9e\u9a8c\u5ba4", "1.6\u4e07+", "\u76f4\u64ad", "\u76f4\u64ad\u642d\u5efa", 184, "#6B78FF", "#283069", "#BFD6FF"),
        card("12", "\u8fde\u9ea6\u4e0d\u51b7\u573a\u7684\u4e94\u4e2a\u8bdd\u9898\u6a21\u677f", "\u4e39\u5b50\u4e3b\u64ad\u8bf4", "4398", "\u6280\u5de7", "\u8fde\u9ea6\u6a21\u677f", 184, "#4DAB98", "#205047", "#D6F2EA"),
        card("13", "\u7f8e\u5986\u5e26\u8d27\u590d\u76d8\uff0c\u505c\u7559\u65f6\u957f\u63d0\u5347 22%", "Cici", "654", "\u6570\u636e", "\u590d\u76d8", 184, "#F07C5A", "#683120", "#FFD8C2"),
        card("14", "\u76f4\u64ad\u95f4\u7559\u8a00\u677f\u6392\u7248\uff0c\u4fe1\u606f\u522b\u5806\u592a\u6ee1", "\u50cf\u7d20\u5de5\u574a", "993", "\u89c6\u89c9", "\u7559\u8a00\u677f", 184, "#D46EA0", "#552238", "#F6D0E3")
    )

    private fun dramaItems() = listOf(
        card("15", "\u53cc\u4eba\u53cd\u8f6c\u77ed\u5267\u7684 8 \u4e2a\u955c\u5934\u6a21\u677f", "\u77ed\u5267\u793e", "2.1\u4e07+", "\u77ed\u5267", "\u53cd\u8f6c\u6a21\u677f", 184, "#8A4FFF", "#2E1B52", "#D8C2FF"),
        card("16", "\u6821\u56ed\u9898\u6750\u6700\u5bb9\u6613\u51fa\u5f69\u7684\u662f\u8282\u594f\u611f", "\u5bfc\u6f14\u963f\u6728", "3522", "\u7ecf\u9a8c", "\u8282\u594f\u611f", 184, "#679BE7", "#24426D", "#C5E0FF"),
        card("17", "\u5982\u4f55\u628a 30 \u79d2\u5267\u60c5\u8bb2\u5b8c\u6574", "\u5c0f\u6ee1", "870", "\u521b\u4f5c", "30 \u79d2", 184, "#FF8A65", "#703525", "#FFD7C7"),
        card("18", "\u7ad6\u5c4f\u5bf9\u767d\u522b\u786c\u62cd\uff0c\u6ce8\u610f\u7559\u767d\u548c\u666f\u522b", "Leo Film", "617", "\u62cd\u6444", "\u7ad6\u5c4f\u5bf9\u767d", 184, "#56A68E", "#1F4C40", "#CFEFE3")
    )

    private fun tipsItems() = listOf(
        card("19", "\u505a\u7b14\u8bb0\u522b\u5806\u5b57\uff0c\u4e09\u6bb5\u5f0f\u6700\u5bb9\u6613\u770b\u5b8c", "\u5199\u4f5c\u7ec3\u4e60\u518c", "9112", "\u7ecf\u9a8c", "\u4e09\u6bb5\u5f0f", 184, "#7C90D6", "#303F74", "#D6DEFF"),
        card("20", "\u7528\u7edf\u4e00\u8272\u677f\u505a\u8d26\u53f7\u89c6\u89c9\uff0c\u4e0d\u4f1a\u4e71", "\u6843\u4e50\u4e2d", "2890", "\u6574\u7406", "\u7edf\u4e00\u8272\u677f", 184, "#EB7E92", "#6A2633", "#FFD1DB"),
        card("21", "\u6807\u9898\u5148\u5199\u7ed3\u679c\uff0c\u518d\u5199\u8fc7\u7a0b", "\u5c0f\u7a0b\u540c\u5b66", "1206", "\u65b9\u6cd5", "\u5148\u7ed3\u679c", 184, "#E2A93B", "#6D4A18", "#FFE4AA"),
        card("22", "\u8bc4\u8bba\u533a\u4e92\u52a8\u8981\u7ed9\u7528\u6237\u63a5\u8bdd\u53e3", "\u6728\u5ddd", "774", "\u8fd0\u8425", "\u63a5\u8bdd\u53e3", 184, "#5DAEC2", "#244A54", "#CDEFF6")
    )

    private fun outfitItems() = listOf(
        card("23", "\u5976\u767d\u8272\u901a\u52e4\u7a7f\u642d\uff0c\u5b89\u9759\u4f46\u4e0d\u65e0\u804a", "Rita", "2.3\u4e07+", "\u7a7f\u642d", "\u5976\u767d\u901a\u52e4", 298, "#DCCFC5", "#7B6A60", "#F7E1CF"),
        card("24", "\u725b\u4ed4\u534a\u88d9\u548c\u77ed\u9774\u662f\u6625\u590f\u8fc7\u6e21\u671f\u7b54\u6848", "Momo", "827", "OOTD", "\u534a\u88d9\u77ed\u9774", 252, "#7C92B2", "#2D405E", "#F3C59E"),
        card("25", "\u7070\u7c89\u8272\u7cfb\u53e0\u7a7f\uff0c\u62cd\u7167\u5f88\u4e0a\u955c", "Annie", "5388", "\u62cd\u7167", "\u7070\u7c89\u53e0\u7a7f", 286, "#B38D99", "#50353F", "#F2CAD6"),
        card("26", "\u5468\u672b\u53bb\u7f8e\u672f\u9986\uff0c\u8fd9\u5957\u9ed1\u767d\u642d\u592a\u591f\u7528\u4e86", "Lynn", "1603", "\u901a\u52e4", "\u9ed1\u767d\u642d", 232, "#4C4C53", "#191A1F", "#D8D8D8")
    )

    private fun foodItems() = listOf(
        card("27", "\u5bb6\u5e38\u5c0f\u9986\u83dc\u5355\u91cd\u6392\uff0c\u62db\u724c\u83dc\u5fc5\u987b\u653e\u5de6\u4e0a", "\u963f\u798f\u9910\u8bb0", "1.1\u4e07+", "\u7f8e\u98df", "\u62db\u724c\u83dc\u5355", 184, "#D46545", "#5F271A", "#FFD3B9"),
        card("28", "\u62b9\u8336\u51b0\u548c\u828b\u6ce5\u51b0\uff0c\u590f\u5929\u5e97\u91cc\u6700\u7a33\u7684\u4e24\u6b3e", "\u751c\u54c1\u5b9e\u9a8c\u5ba4", "669", "\u65b0\u54c1", "\u62b9\u8336\u82cb\u6ce5", 184, "#79A26B", "#31472A", "#E4F0C8"),
        card("29", "\u65e9\u9910\u5e97\u4e5f\u80fd\u505a\u9ad8\u7ea7\u611f\u6d77\u62a5", "\u7ae0\u9c7c\u89c6\u89c9", "905", "\u8bbe\u8ba1", "\u65e9\u9910\u6d77\u62a5", 184, "#F0A44E", "#734A18", "#FFE0AF"),
        card("30", "\u4e00\u955c\u5230\u5e95\u62cd\u51fa\u70ed\u6c14\u817e\u817e\u7684\u53a8\u623f\u611f", "\u8001\u8d75\u4e0b\u53a8", "1530", "\u62cd\u6444", "\u53a8\u623f\u611f", 184, "#E57962", "#662E23", "#FFD7C7")
    )

    private fun travelItems() = listOf(
        card("31", "\u4e91\u5357\u96e8\u5b63\u4e0d\u5e9f\u7247\uff0c\u53cd\u800c\u66f4\u6709\u6c1b\u56f4", "\u963f\u9065\u65c5\u884c", "9600", "\u65c5\u884c", "\u96e8\u5b63\u6c1b\u56f4", 184, "#5D8E95", "#1F3B43", "#BEE5E3"),
        card("32", "\u57ce\u5e02\u591c\u6e38\u8def\u7ebf\u56fe\uff0c\u4e09\u5c0f\u65f6\u521a\u597d\u8d70\u5b8c", "Maps Lab", "1418", "\u8def\u7ebf", "\u591c\u6e38", 184, "#5975C8", "#21305B", "#CAD7FF"),
        card("33", "\u884c\u674e\u53ea\u5e26\u4e00\u4e2a\u767b\u673a\u7bb1\u4e5f\u591f\u7528", "\u5c0f\u7941", "507", "\u51fa\u884c", "\u8f7b\u88c5", 184, "#9D7DD3", "#392B58", "#E0D3FF"),
        card("34", "\u6d77\u8fb9\u843d\u65e5\u600e\u4e48\u62cd\u90fd\u50cf\u660e\u4fe1\u7247", "Dora", "2011", "\u6444\u5f71", "\u6d77\u8fb9\u843d\u65e5", 184, "#F39557", "#6A3820", "#FFD6B5")
    )

    private fun card(
        id: String,
        title: String,
        author: String,
        likeCount: String,
        badge: String,
        coverLabel: String,
        coverHeightDp: Int,
        startColorHex: String,
        endColorHex: String,
        avatarColorHex: String
    ) = HomeCardItem(
        id = id,
        title = title,
        author = author,
        likeCount = likeCount,
        badge = badge,
        coverLabel = coverLabel,
        coverHeightDp = coverHeightDp,
        startColorHex = startColorHex,
        endColorHex = endColorHex,
        avatarColorHex = avatarColorHex
    )
}
