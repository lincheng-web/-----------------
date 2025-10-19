import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 热播电视剧评价及推荐系统
 * 该系统允许用户对电视剧进行评分和评价，并提供AI推荐功能
 */
public class TVShowRecommendationSystem {
    // 数据文件路径
    private static final String USERS_FILE = "users.txt";
    private static final String TV_SHOWS_FILE = "tv_shows.txt";
    private static final String RATINGS_FILE = "ratings.txt";
    
    // DeepSeek API配置 - 请替换为实际的API密钥
    private static final String DEEPSEEK_API_KEY = "sk-49deaf4074014933a71cc28fba591832";
    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/chat/completions";
    
    // 当前登录用户
    private static User currentUser = null;
    
    // 数据集合
    private static List<User> users = new ArrayList<>();
    private static List<TVShow> tvShows = new ArrayList<>();
    private static List<Rating> ratings = new ArrayList<>();
    
    // 密码输入时的掩码字符
    private static final char MASK_CHAR = '*';
    
    /**
     * 用户类
     */
    static class User {
        private String username;    // 用户名
        private String password;    // 密码
        private String role;        // 角色：admin 或 user
        private String preferences; // 用户喜好
        
        /**
         * 构造函数
         * @param username 用户名
         * @param password 密码
         * @param role 角色
         */
        public User(String username, String password, String role) {
            this.username = username;
            this.password = password;
            this.role = role;
            this.preferences = "";
        }
        
        /**
         * 带喜好的构造函数
         */
        public User(String username, String password, String role, String preferences) {
            this.username = username;
            this.password = password;
            this.role = role;
            this.preferences = preferences;
        }
        
        // Getter和Setter方法
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getRole() { return role; }
        public String getPreferences() { return preferences; }
        public void setPreferences(String preferences) { this.preferences = preferences; }
    }
    
    /**
     * 电视剧类
     */
    static class TVShow {
        private String id;              // 电视剧ID
        private String title;           // 标题
        private String genre;           // 类型
        private int year;               // 上映年份
        private String director;        // 导演
        private List<String> actors;     // 演员列表
        private String description;     // 描述
        private double rating;          // 平均评分
        private int ratingCount;        // 评分人数
        private int viewCount;          // 观看次数
        
        /**
         * 构造函数
         */
        public TVShow(String id, String title, String genre, int year, String director, 
                     List<String> actors, String description) {
            this.id = id;
            this.title = title;
            this.genre = genre;
            this.year = year;
            this.director = director;
            this.actors = actors;
            this.description = description;
            this.rating = 0.0;
            this.ratingCount = 0;
            this.viewCount = 0;
        }
        
        // Getter和Setter方法
        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getGenre() { return genre; }
        public int getYear() { return year; }
        public String getDirector() { return director; }
        public List<String> getActors() { return actors; }
        public String getDescription() { return description; }
        public double getRating() { return rating; }
        public int getRatingCount() { return ratingCount; }
        public int getViewCount() { return viewCount; }
        
        public void setTitle(String title) { this.title = title; }
        public void setGenre(String genre) { this.genre = genre; }
        public void setYear(int year) { this.year = year; }
        public void setDirector(String director) { this.director = director; }
        public void setActors(List<String> actors) { this.actors = actors; }
        public void setDescription(String description) { this.description = description; }
        
        /**
         * 更新评分
         */
        public void updateRating(double newRating) {
            this.rating = (this.rating * this.ratingCount + newRating) / (this.ratingCount + 1);
            this.ratingCount++;
        }
        
        /**
         * 增加观看次数
         */
        public void incrementViewCount() {
            this.viewCount++;
        }
        
        /**
         * 转换为用于推荐的字符串描述
         */
        public String toRecommendationString() {
            return String.format("ID: %s, 标题: %s, 类型: %s, 年份: %d, 导演: %s, 演员: %s, 评分: %.1f",
                    id, title, genre, year, director, String.join(",", actors), rating);
        }
    }
    
    /**
     * 评分评价类
     */
    static class Rating {
        private String id;              // 评分ID
        private String tvShowId;        // 电视剧ID
        private String username;        // 用户名
        private double score;           // 评分（1-5）
        private String comment;         // 评论
        private String timestamp;       // 时间戳
        
        /**
         * 构造函数
         */
        public Rating(String id, String tvShowId, String username, double score, String comment) {
            this.id = id;
            this.tvShowId = tvShowId;
            this.username = username;
            this.score = score;
            this.comment = comment;
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        
        // Getter方法
        public String getId() { return id; }
        public String getTvShowId() { return tvShowId; }
        public String getUsername() { return username; }
        public double getScore() { return score; }
        public String getComment() { return comment; }
        public String getTimestamp() { return timestamp; }
    }
    
    /**
     * 主方法
     */
    public static void main(String[] args) {
        // 加载数据
        loadData();
        
        // 显示欢迎信息
        System.out.println("=====================================");
        System.out.println("  热播电视剧评价及推荐系统");
        System.out.println("=====================================");
        
        // 主菜单循环
        while (true) {
            if (currentUser == null) {
                showLoginMenu();
            } else {
                if (currentUser.getRole().equals("admin")) {
                    showAdminMenu();
                } else {
                    // 普通用户登录后先显示推荐入口菜单
                    showPostLoginMenu();
                }
            }
        }
    }
    
    /**
     * 用户登录后的初始菜单（包含推荐入口）
     */
    private static void showPostLoginMenu() {
        System.out.println("\n========== 欢迎回来，" + currentUser.getUsername() + " ==========");
        System.out.println("1. 获取个性化电视剧推荐");
        System.out.println("2. 进入系统主菜单");
        System.out.println("3. 退出登录");
        System.out.print("请选择操作: ");
        
        Scanner scanner = new Scanner(System.in);
        int choice = scanner.nextInt();
        scanner.nextLine(); // 消耗换行符
        
        switch (choice) {
            case 1:
                getRecommendations();
                // 推荐完成后返回此菜单
                showPostLoginMenu();
                break;
            case 2:
                showUserMenu();
                break;
            case 3:
                currentUser = null;
                System.out.println("已退出登录！");
                break;
            default:
                System.out.println("无效的选择，请重新输入！");
                showPostLoginMenu();
        }
    }
    
    /**
     * 加载所有数据
     */
    private static void loadData() {
        loadUsers();
        loadTVShows();
        loadRatings();
    }
    
    /**
     * 加载用户数据
     */
    private static void loadUsers() {
        try (BufferedReader br = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|");
                if (parts.length == 3) {
                    users.add(new User(parts[0], parts[1], parts[2]));
                } else if (parts.length == 4) {
                    // 支持带有喜好的用户数据
                    users.add(new User(parts[0], parts[1], parts[2], parts[3]));
                }
            }
        } catch (FileNotFoundException e) {
            // 文件不存在，使用默认管理员账户
            users.add(new User("admin", "admin123", "admin"));
            saveUsers();
            System.out.println("用户文件不存在，已创建默认管理员账户(admin/admin123)");
        } catch (IOException e) {
            System.out.println("加载用户数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 加载电视剧数据
     */
    private static void loadTVShows() {
        try (BufferedReader br = new BufferedReader(new FileReader(TV_SHOWS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|");
                if (parts.length >= 7) {
                    String id = parts[0];
                    String title = parts[1];
                    String genre = parts[2];
                    int year = Integer.parseInt(parts[3]);
                    String director = parts[4];
                    List<String> actors = Arrays.asList(parts[5].split(","));
                    String description = parts[6];
                    
                    TVShow tvShow = new TVShow(id, title, genre, year, director, actors, description);
                    
                    // 加载评分和观看次数
                    if (parts.length >= 9) {
                        tvShow.rating = Double.parseDouble(parts[7]);
                        tvShow.ratingCount = Integer.parseInt(parts[8]);
                    }
                    if (parts.length >= 10) {
                        tvShow.viewCount = Integer.parseInt(parts[9]);
                    }
                    
                    tvShows.add(tvShow);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("电视剧文件不存在，将创建新文件");
        } catch (IOException e) {
            System.out.println("加载电视剧数据失败: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("电视剧数据格式错误: " + e.getMessage());
        }
    }
    
    /**
     * 加载评分数据
     */
    private static void loadRatings() {
        try (BufferedReader br = new BufferedReader(new FileReader(RATINGS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|");
                if (parts.length >= 5) {
                    String id = parts[0];
                    String tvShowId = parts[1];
                    String username = parts[2];
                    double score = Double.parseDouble(parts[3]);
                    String comment = parts[4];
                    
                    Rating rating = new Rating(id, tvShowId, username, score, comment);
                    
                    // 加载时间戳
                    if (parts.length >= 6) {
                        rating.timestamp = parts[5];
                    }
                    
                    ratings.add(rating);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("评分文件不存在，将创建新文件");
        } catch (IOException e) {
            System.out.println("加载评分数据失败: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("评分数据格式错误: " + e.getMessage());
        }
    }
    
    /**
     * 保存所有数据
     */
    private static void saveData() {
        saveUsers();
        saveTVShows();
        saveRatings();
    }
    
    /**
     * 保存用户数据
     */
    private static void saveUsers() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(USERS_FILE))) {
            for (User user : users) {
                bw.write(user.getUsername() + "|" + user.getPassword() + "|" + user.getRole() + "|" + user.getPreferences());
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("保存用户数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存电视剧数据
     */
    private static void saveTVShows() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(TV_SHOWS_FILE))) {
            for (TVShow tvShow : tvShows) {
                String actors = String.join(",", tvShow.getActors());
                bw.write(tvShow.getId() + "|" + tvShow.getTitle() + "|" + tvShow.getGenre() + "|" +
                        tvShow.getYear() + "|" + tvShow.getDirector() + "|" + actors + "|" +
                        tvShow.getDescription() + "|" + tvShow.getRating() + "|" +
                        tvShow.getRatingCount() + "|" + tvShow.getViewCount());
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("保存电视剧数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存评分数据
     */
    private static void saveRatings() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(RATINGS_FILE))) {
            for (Rating rating : ratings) {
                bw.write(rating.getId() + "|" + rating.getTvShowId() + "|" + rating.getUsername() + "|" +
                        rating.getScore() + "|" + rating.getComment() + "|" + rating.getTimestamp());
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("保存评分数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 显示登录菜单
     */
    private static void showLoginMenu() {
        System.out.println("\n========== 登录菜单 ==========");
        System.out.println("1. 用户登录");
        System.out.println("2. 用户注册");
        System.out.println("3. 退出系统");
        System.out.print("请选择操作: ");
        
        Scanner scanner = new Scanner(System.in);
        int choice = scanner.nextInt();
        scanner.nextLine(); // 消耗换行符
        
        switch (choice) {
            case 1:
                login();
                break;
            case 2:
                register();
                break;
            case 3:
                System.out.println("谢谢使用，再见！");
                System.exit(0);
                break;
            default:
                System.out.println("无效的选择，请重新输入！");
        }
    }
    
    /**
     * 用户登录
     */
    private static void login() {
        System.out.println("\n========== 用户登录 ==========");
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("请输入用户名: ");
        String username = scanner.nextLine();
        
        System.out.print("请输入密码: ");
        String password = readPassword();
        
        // 检查是否为管理员
        if (username.equals("admin")) {
            int failedAttempts = 0;
            while (failedAttempts < 3) {
                User admin = findUserByUsername("admin");
                if (admin != null && password.equals(admin.getPassword())) {
                    currentUser = admin;
                    System.out.println("管理员登录成功！");
                    return;
                } else {
                    failedAttempts++;
                    System.out.println("密码错误！剩余尝试次数: " + (3 - failedAttempts));
                    if (failedAttempts < 3) {
                        System.out.print("请重新输入密码: ");
                        password = readPassword();
                    }
                }
            }
            System.out.println("密码错误次数过多，系统已锁定！");
            System.exit(0);
        } else {
            // 普通用户登录
            User user = findUserByUsername(username);
            if (user != null && password.equals(user.getPassword())) {
                currentUser = user;
                System.out.println("登录成功！欢迎回来，" + username + "！");
            } else {
                System.out.println("用户名或密码错误！");
            }
        }
    }
    
    /**
     * 用户注册
     */
    private static void register() {
        System.out.println("\n========== 用户注册 ==========");
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("请输入用户名: ");
        String username = scanner.nextLine();
        
        // 检查用户名是否已存在
        if (findUserByUsername(username) != null) {
            System.out.println("用户名已存在，请重新选择！");
            return;
        }
        
        System.out.print("请输入密码: ");
        String password = readPassword();
        
        System.out.print("请再次输入密码: ");
        String confirmPassword = readPassword();
        
        if (!password.equals(confirmPassword)) {
            System.out.println("两次输入的密码不一致！");
            return;
        }
        
        // 创建新用户
        User newUser = new User(username, password, "user");
        users.add(newUser);
        saveUsers();
        
        System.out.println("注册成功！");
    }
    
    /**
     * 读取密码（掩码显示）
     */
    private static String readPassword() {
        StringBuilder password = new StringBuilder();
        try {
            // 禁用系统输入的回显
            System.out.print("\033[8m"); // 隐藏输入（部分终端支持）
            
            while (true) {
                // 读取单个字符
                int c = System.in.read();
                // 处理回车或换行，结束输入
                if (c == '\n' || c == '\r') {
                    System.out.println("\033[0m"); // 恢复显示
                    break;
                }
                // 处理退格键
                else if (c == 127 || c == 8) { // 127是Delete，8是Backspace
                    if (password.length() > 0) {
                        // 删除密码中的最后一个字符
                        password.deleteCharAt(password.length() - 1);
                        // 在控制台删除显示的*
                        System.out.print("\b \b");
                    }
                }
                // 处理普通字符
                else {
                    password.append((char) c);
                    System.out.print(MASK_CHAR); // 显示掩码
                }
            }
        } catch (IOException e) {
            System.out.println("输入密码时发生错误: " + e.getMessage());
        }
        return password.toString();
    }
    
    /**
     * 根据用户名查找用户
     */
    private static User findUserByUsername(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }
    
    /**
     * 查看用户评价（管理员）
     */
    private static void viewUserRatings() {
        System.out.println("\n========== 查看用户评价 ==========");
        System.out.println("1. 查看所有评价");
        System.out.println("2. 按电视剧查看评价");
        System.out.println("3. 按用户查看评价");
        System.out.print("请选择操作: ");
        
        Scanner scanner = new Scanner(System.in);
        int choice = scanner.nextInt();
        scanner.nextLine(); // 消耗换行符
        
        List<Rating> results = new ArrayList<>();
        
        switch (choice) {
            case 1:
                results = ratings;
                break;
            case 2:
                System.out.print("请输入电视剧ID: ");
                String tvShowId = scanner.nextLine();
                results = findRatingsByTVShowId(tvShowId);
                break;
            case 3:
                System.out.print("请输入用户名: ");
                String username = scanner.nextLine();
                results = findRatingsByUsername(username);
                break;
            default:
                System.out.println("无效的选择！");
                return;
        }
        
        if (results.isEmpty()) {
            System.out.println("没有找到评价！");
        } else {
            System.out.println("\n评价列表:");
            for (Rating rating : results) {
                TVShow tvShow = findTVShowById(rating.getTvShowId());
                if (tvShow != null) {
                    System.out.println("电视剧: " + tvShow.getTitle());
                    System.out.println("用户: " + rating.getUsername());
                    System.out.println("评分: " + rating.getScore() + "星");
                    System.out.println("评价: " + rating.getComment());
                    System.out.println("时间: " + rating.getTimestamp());
                    System.out.println("-------------------------------------");
                }
            }
        }
    }
    
    /**
     * 查看热播电视剧榜
     */
    private static void viewTop10TVShows() {
        System.out.println("\n========== 热播电视剧榜 ==========");
        
        // 复制列表以避免修改原始数据
        List<TVShow> sortedTVShows = new ArrayList<>(tvShows);
        
        // 按观看次数排序
        Collections.sort(sortedTVShows, new Comparator<TVShow>() {
            @Override
            public int compare(TVShow o1, TVShow o2) {
                return Integer.compare(o2.getViewCount(), o1.getViewCount());
            }
        });
        
        // 取前10名
        int limit = Math.min(10, sortedTVShows.size());
        for (int i = 0; i < limit; i++) {
            TVShow tvShow = sortedTVShows.get(i);
            System.out.println((i + 1) + ". " + tvShow.getTitle() + 
                             " (评分: " + String.format("%.1f", tvShow.getRating()) + 
                             ", 观看次数: " + tvShow.getViewCount() + ")");
        }
        
        // 询问是否查看详情
        System.out.print("\n是否查看某部电视剧的详细信息？(Y/N): ");
        Scanner scanner = new Scanner(System.in);
        String choice = scanner.nextLine();
        
        if (choice.equalsIgnoreCase("Y")) {
            System.out.print("请输入电视剧序号: ");
            try {
                int index = scanner.nextInt() - 1;
                scanner.nextLine(); // 消耗换行符
                
                if (index >= 0 && index < limit) {
                    TVShow selected = sortedTVShows.get(index);
                    displayTVShowDetails(selected);
                    
                    // 增加观看次数
                    selected.incrementViewCount();
                    saveTVShows();
                } else {
                    System.out.println("无效的序号！");
                }
            } catch (NumberFormatException e) {
                System.out.println("无效的输入！");
            }
        }
    }
    
    /**
     * 显示管理员菜单
     */
    private static void showAdminMenu() {
        System.out.println("\n========== 管理员菜单 ==========");
        System.out.println("1. 电视剧管理");
        System.out.println("2. 查看用户评价");
        System.out.println("3. 查看热播电视剧榜");
        System.out.println("4. 退出登录");
        System.out.print("请选择操作: ");
        
        Scanner scanner = new Scanner(System.in);
        int choice = scanner.nextInt();
        scanner.nextLine(); // 消耗换行符
        
        switch (choice) {
            case 1:
                manageTVShows();
                break;
            case 2:
                viewUserRatings();
                break;
            case 3:
                viewTop10TVShows();
                break;
            case 4:
                currentUser = null;
                System.out.println("已退出登录！");
                break;
            default:
                System.out.println("无效的选择，请重新重新输入！");
        }
    }
    
    /**
     * 电视剧管理功能
     */
    private static void manageTVShows() {
        while (true) {
            System.out.println("\n========== 电视剧管理 ==========");
            System.out.println("1. 查询电视剧");
            System.out.println("2. 添加电视剧");
            System.out.println("3. 修改电视剧");
            System.out.println("4. 删除电视剧");
            System.out.println("5. 返回上一级");
            System.out.print("请选择操作: ");
            
            Scanner scanner = new Scanner(System.in);
            int choice = scanner.nextInt();
            scanner.nextLine(); // 消耗换行符
            
            switch (choice) {
                case 1:
                    adminSearchTVShows();
                    break;
                case 2:
                    addTVShow();
                    break;
                case 3:
                    updateTVShow();
                    break;
                case 4:
                    deleteTVShow();
                    break;
                case 5:
                    return;
                default:
                    System.out.println("无效的选择，请重新输入！");
            }
        }
    }
    
    /**
     * 管理员查询电视剧
     */
    private static void adminSearchTVShows() {
        System.out.println("\n========== 查询电视剧 ==========");
        System.out.println("1. 按ID查询");
        System.out.println("2. 按标题查询");
        System.out.println("3. 按类型查询");
        System.out.println("4. 查看所有电视剧");
        System.out.print("请选择查询方式: ");
        
        Scanner scanner = new Scanner(System.in);
        int choice = scanner.nextInt();
        scanner.nextLine(); // 消耗换行符
        
        List<TVShow> results = new ArrayList<>();
        
        switch (choice) {
            case 1:
                System.out.print("请输入电视剧ID: ");
                String id = scanner.nextLine();
                TVShow tvShow = findTVShowById(id);
                if (tvShow != null) {
                    results.add(tvShow);
                }
                break;
            case 2:
                System.out.print("请输入电视剧标题: ");
                String title = scanner.nextLine();
                results = findTVShowsByTitle(title);
                break;
            case 3:
                System.out.print("请输入电视剧类型: ");
                String genre = scanner.nextLine();
                results = findTVShowsByGenre(genre);
                break;
            case 4:
                results = tvShows;
                break;
            default:
                System.out.println("无效的选择！");
                return;
        }
        
        if (results.isEmpty()) {
            System.out.println("没有找到匹配的电视剧！");
        } else {
            System.out.println("\n查询结果:");
            for (TVShow show : results) {
                displayTVShowDetails(show);
                System.out.println("-------------------------------------");
            }
        }
    }
    
    /**
     * 添加电视剧
     */
    private static void addTVShow() {
        System.out.println("\n========== 添加电视剧 ==========");
        Scanner scanner = new Scanner(System.in);
        
        // 生成ID
        String id = generateTVShowId();
        
        System.out.print("请输入电视剧标题: ");
        String title = scanner.nextLine();
        
        System.out.print("请输入电视剧类型: ");
        String genre = scanner.nextLine();
        
        System.out.print("请输入上映年份: ");
        int year = scanner.nextInt();
        scanner.nextLine(); // 消耗换行符
        
        System.out.print("请输入导演: ");
        String director = scanner.nextLine();
        
        System.out.print("请输入演员（用逗号分隔）: ");
        String actorsInput = scanner.nextLine();
        List<String> actors = Arrays.asList(actorsInput.split(","));
        
        System.out.print("请输入剧情描述: ");
        String description = scanner.nextLine();
        
        // 创建电视剧对象
        TVShow newTVShow = new TVShow(id, title, genre, year, director, actors, description);
        tvShows.add(newTVShow);
        
        // 保存数据
        saveTVShows();
        
        System.out.println("电视剧添加成功！");
        displayTVShowDetails(newTVShow);
    }
    
    /**
     * 修改电视剧
     */
    private static void updateTVShow() {
        System.out.println("\n========== 修改电视剧 ==========");
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("请输入要修改的电视剧ID: ");
        String id = scanner.nextLine();
        
        TVShow tvShow = findTVShowById(id);
        if (tvShow == null) {
            System.out.println("找不到该电视剧！");
            return;
        }
        
        System.out.println("当前电视剧信息:");
        displayTVShowDetails(tvShow);
        
        System.out.println("\n请输入新的信息（不修改的项按回车跳过）:");
        
        System.out.print("标题 (" + tvShow.getTitle() + "): ");
        String title = scanner.nextLine();
        if (!title.isEmpty()) {
            tvShow.setTitle(title);
        }
        
        System.out.print("类型 (" + tvShow.getGenre() + "): ");
        String genre = scanner.nextLine();
        if (!genre.isEmpty()) {
            tvShow.setGenre(genre);
        }
        
        System.out.print("上映年份 (" + tvShow.getYear() + "): ");
        String yearInput = scanner.nextLine();
        if (!yearInput.isEmpty()) {
            try {
                int year = Integer.parseInt(yearInput);
                tvShow.setYear(year);
            } catch (NumberFormatException e) {
                System.out.println("年份格式错误，保持原值！");
            }
        }
        
        System.out.print("导演 (" + tvShow.getDirector() + "): ");
        String director = scanner.nextLine();
        if (!director.isEmpty()) {
            tvShow.setDirector(director);
        }
        
        System.out.print("演员 (" + String.join(",", tvShow.getActors()) + "): ");
        String actorsInput = scanner.nextLine();
        if (!actorsInput.isEmpty()) {
            List<String> actors = Arrays.asList(actorsInput.split(","));
            tvShow.setActors(actors);
        }
        
        System.out.print("剧情描述 (" + tvShow.getDescription() + "): ");
        String description = scanner.nextLine();
        if (!description.isEmpty()) {
            tvShow.setDescription(description);
        }
        
        // 保存数据
        saveTVShows();
        
        System.out.println("电视剧信息修改成功！");
        displayTVShowDetails(tvShow);
    }
    
    /**
     * 删除电视剧
     */
    private static void deleteTVShow() {
        System.out.println("\n========== 删除电视剧 ==========");
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("请输入要删除的电视剧ID: ");
        String id = scanner.nextLine();
        
        TVShow tvShow = findTVShowById(id);
        if (tvShow == null) {
            System.out.println("找不到该电视剧！");
            return;
        }
        
        System.out.println("确认要删除以下电视剧吗？");
        displayTVShowDetails(tvShow);
        System.out.print("请输入 Y 确认删除，其他键取消: ");
        String confirm = scanner.nextLine();
        
        if (confirm.equalsIgnoreCase("Y")) {
            // 删除电视剧
            tvShows.remove(tvShow);
            
            // 删除相关评分
            Iterator<Rating> iterator = ratings.iterator();
            while (iterator.hasNext()) {
                Rating rating = iterator.next();
                if (rating.getTvShowId().equals(id)) {
                    iterator.remove();
                }
            }
            
            // 保存数据
            saveTVShows();
            saveRatings();
            
            System.out.println("电视剧删除成功！");
        } else {
            System.out.println("删除操作已取消！");
        }
    }
    
    /**
     * 生成电视剧ID
     */
    private static String generateTVShowId() {
        int maxId = 0;
        for (TVShow tvShow : tvShows) {
            try {
                int id = Integer.parseInt(tvShow.getId().substring(2)); // 假设ID格式为"TV001"
                if (id > maxId) {
                    maxId = id;
                }
            } catch (NumberFormatException e) {
                // 如果ID格式不是预期的，忽略
            }
        }
        return String.format("TV%03d", maxId + 1);
    }
    
    /**
     * 根据ID查找电视剧
     */
    private static TVShow findTVShowById(String id) {
        for (TVShow tvShow : tvShows) {
            if (tvShow.getId().equals(id)) {
                return tvShow;
            }
        }
        return null;
    }
    
    /**
     * 根据标题查找电视剧
     */
    private static List<TVShow> findTVShowsByTitle(String title) {
        List<TVShow> results = new ArrayList<>();
        for (TVShow tvShow : tvShows) {
            if (tvShow.getTitle().toLowerCase().contains(title.toLowerCase())) {
                results.add(tvShow);
            }
        }
        return results;
    }
    
    /**
     * 根据类型查找电视剧
     */
    private static List<TVShow> findTVShowsByGenre(String genre) {
        List<TVShow> results = new ArrayList<>();
        for (TVShow tvShow : tvShows) {
            if (tvShow.getGenre().toLowerCase().contains(genre.toLowerCase())) {
                results.add(tvShow);
            }
        }
        return results;
    }
    
    /**
     * 显示电视剧详细信息
     */
    private static void displayTVShowDetails(TVShow tvShow) {
        System.out.println("ID: " + tvShow.getId());
        System.out.println("标题: " + tvShow.getTitle());
        System.out.println("类型: " + tvShow.getGenre());
        System.out.println("上映年份: " + tvShow.getYear());
        System.out.println("导演: " + tvShow.getDirector());
        System.out.println("演员: " + String.join(", ", tvShow.getActors()));
        System.out.println("评分: " + String.format("%.1f", tvShow.getRating()) + " (" + tvShow.getRatingCount() + "人评分)");
        System.out.println("观看次数: " + tvShow.getViewCount());
        System.out.println("剧情描述: " + tvShow.getDescription());
    }
    
    /**
     * 用户查询电视剧
     */
    private static void searchTVShows() {
        System.out.println("\n========== 查询电视剧 ==========");
        System.out.println("1. 按ID查询");
        System.out.println("2. 按标题查询");
        System.out.println("3. 按类型查询");
        System.out.println("4. 查看所有电视剧");
        System.out.print("请选择查询方式: ");
        
        Scanner scanner = new Scanner(System.in);
        int choice = scanner.nextInt();
        scanner.nextLine(); // 消耗换行符
        
        List<TVShow> results = new ArrayList<>();
        
        switch (choice) {
            case 1:
                System.out.print("请输入电视剧ID: ");
                String id = scanner.nextLine();
                TVShow tvShow = findTVShowById(id);
                if (tvShow != null) {
                    results.add(tvShow);
                }
                break;
            case 2:
                System.out.print("请输入电视剧标题: ");
                String title = scanner.nextLine();
                results = findTVShowsByTitle(title);
                break;
            case 3:
                System.out.print("请输入电视剧类型: ");
                String genre = scanner.nextLine();
                results = findTVShowsByGenre(genre);
                break;
            case 4:
                results = tvShows;
                break;
            default:
                System.out.println("无效的选择！");
                return;
        }
        
        if (results.isEmpty()) {
            System.out.println("没有找到匹配的电视剧！");
        } else {
            System.out.println("\n查询结果:");
            for (int i = 0; i < results.size(); i++) {
                TVShow show = results.get(i);
                System.out.println((i + 1) + ". " + show.getTitle() + 
                                 " (ID: " + show.getId() + 
                                 ", 评分: " + String.format("%.1f", show.getRating()) + ")");
            }
            
            // 询问是否查看详情
            System.out.print("\n是否查看某部电视剧的详细信息？(Y/N): ");
            String detailChoice = scanner.nextLine();
            
            if (detailChoice.equalsIgnoreCase("Y")) {
                System.out.print("请输入电视剧序号: ");
                try {
                    int index = scanner.nextInt() - 1;
                    scanner.nextLine(); // 消耗换行符
                    
                    if (index >= 0 && index < results.size()) {
                        TVShow selected = results.get(index);
                        displayTVShowDetails(selected);
                        
                        // 增加观看次数
                        selected.incrementViewCount();
                        saveTVShows();
                    } else {
                        System.out.println("无效的序号！");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("无效的输入！");
                }
            }
        }
    }
    
    /**
     * 对电视剧进行评分和评价
     */
    private static void rateTVShow() {
        System.out.println("\n========== 评分与评价 ==========");
        
        // 先让用户查询电视剧
        searchTVShows();
        
        Scanner scanner = new Scanner(System.in);
        System.out.print("\n请输入要评分的电视剧ID: ");
        String tvShowId = scanner.nextLine();
        
        TVShow tvShow = findTVShowById(tvShowId);
        if (tvShow == null) {
            System.out.println("找不到该电视剧！");
            return;
        }
        
        // 检查用户是否已经评分过
        Rating existingRating = findRatingByUserAndTVShow(currentUser.getUsername(), tvShowId);
        if (existingRating != null) {
            System.out.println("您已经对这部电视剧评分过了！");
            System.out.println("当前评分: " + existingRating.getScore() + "星");
            System.out.println("当前评价: " + existingRating.getComment());
            
            System.out.print("是否修改评分和评价？(Y/N): ");
            String modifyChoice = scanner.nextLine();
            
            if (!modifyChoice.equalsIgnoreCase("Y")) {
                return;
            }
            
            // 删除旧评分
            ratings.remove(existingRating);
        }
        
        // 输入新评分
        System.out.print("请输入评分（1-5星）: ");
        double score;
        try {
            score = scanner.nextDouble();
            scanner.nextLine(); // 消耗换行符
            
            if (score < 1 || score > 5) {
                System.out.println("评分必须在1-5之间！");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("无效的评分！");
            return;
        }
        
        // 输入评价
        System.out.print("请输入评价: ");
        String comment = scanner.nextLine();
        
        // 创建新评分
        String ratingId = generateRatingId();
        Rating newRating = new Rating(ratingId, tvShowId, currentUser.getUsername(), score, comment);
        ratings.add(newRating);
        
        // 更新电视剧评分
        tvShow.updateRating(score);
        
        // 保存数据
        saveTVShows();
        saveRatings();
        
        System.out.println("评分和评价提交成功！");
    }
    
    /**
     * 查看电视剧评价
     */
    private static void viewTVShowRatings() {
        System.out.println("\n========== 查看电视剧评价 ==========");
        
        // 先让用户查询电视剧
        searchTVShows();
        
        Scanner scanner = new Scanner(System.in);
        System.out.print("\n请输入要查看评价的电视剧ID: ");
        String tvShowId = scanner.nextLine();
        
        TVShow tvShow = findTVShowById(tvShowId);
        if (tvShow == null) {
            System.out.println("找不到该电视剧！");
            return;
        }
        
        System.out.println("\n电视剧: " + tvShow.getTitle());
        System.out.println("平均评分: " + String.format("%.1f", tvShow.getRating()) + " (" + tvShow.getRatingCount() + "人评分)");
        
        List<Rating> tvShowRatings = findRatingsByTVShowId(tvShowId);
        if (tvShowRatings.isEmpty()) {
            System.out.println("暂无评价！");
        } else {
            System.out.println("\n评价列表:");
            for (Rating rating : tvShowRatings) {
                System.out.println("用户: " + rating.getUsername());
                System.out.println("评分: " + rating.getScore() + "星");
                System.out.println("评价: " + rating.getComment());
                System.out.println("时间: " + rating.getTimestamp());
                System.out.println("-------------------------------------");
            }
        }
    }
    
    /**
     * 根据电视剧ID查找评分
     */
    private static List<Rating> findRatingsByTVShowId(String tvShowId) {
        List<Rating> results = new ArrayList<>();
        for (Rating rating : ratings) {
            if (rating.getTvShowId().equals(tvShowId)) {
                results.add(rating);
            }
        }
        return results;
    }
    
    /**
     * 根据用户名查找评分
     */
    private static List<Rating> findRatingsByUsername(String username) {
        List<Rating> results = new ArrayList<>();
        for (Rating rating : ratings) {
            if (rating.getUsername().equals(username)) {
                results.add(rating);
            }
        }
        return results;
    }
    
    /**
     * 根据用户和电视剧ID查找评分
     */
    private static Rating findRatingByUserAndTVShow(String username, String tvShowId) {
        for (Rating rating : ratings) {
            if (rating.getUsername().equals(username) && rating.getTvShowId().equals(tvShowId)) {
                return rating;
            }
        }
        return null;
    }
    
    /**
     * 生成评分ID
     */
    private static String generateRatingId() {
        int maxId = 0;
        for (Rating rating : ratings) {
            try {
                int id = Integer.parseInt(rating.getId().substring(2)); // 假设ID格式为"RT001"
                if (id > maxId) {
                    maxId = id;
                }
            } catch (NumberFormatException e) {
                // 如果ID格式不是预期的，忽略
            }
        }
        return String.format("RT%03d", maxId + 1);
    }
    
    /**
     * 设置用户喜好并获取推荐
     */
    private static void getRecommendations() {
        System.out.println("\n========== 电视剧推荐 ==========");
        Scanner scanner = new Scanner(System.in);
        
        // 获取用户喜好
        System.out.println("请告诉我们您的喜好，以便为您推荐合适的电视剧:");
        System.out.println("例如：我喜欢悬疑类型的电视剧，特别是张艺谋导演的作品");
        System.out.print("请输入您的喜好: ");
        String preferences = scanner.nextLine();
        
        // 保存用户喜好
        currentUser.setPreferences(preferences);
        saveUsers();
        
        // 准备推荐请求数据
        System.out.println("\n正在根据您的喜好生成推荐...");
        
        // 构建电视剧库信息
        StringBuilder tvShowsInfo = new StringBuilder();
        tvShowsInfo.append("可用的电视剧列表：\n");
        for (TVShow show : tvShows) {
            tvShowsInfo.append(show.toRecommendationString()).append("\n");
        }
        
        // 构建提示信息
        String prompt = String.format("用户喜好：%s\n%s\n请根据用户的喜好从提供的电视剧列表中推荐3-5部最匹配的电视剧，只返回推荐的电视剧ID和标题，每行一个，不要其他内容。",
                preferences, tvShowsInfo.toString());
        
        try {
            // 调用DeepSeek API获取推荐
            String response = callDeepSeekAPI(prompt);
            
            // 解析并显示推荐结果
            displayRecommendations(response);
            
        } catch (Exception e) {
            System.out.println("获取推荐失败: " + e.getMessage());
            // 提供基于类型的简单备选推荐
            System.out.println("为您提供基于类型的推荐:");
            provideFallbackRecommendations(preferences);
        }
    }
    
    /**
     * 调用DeepSeek API
     */
    private static String callDeepSeekAPI(String prompt) throws IOException {
        URL url = new URL(DEEPSEEK_API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // 设置请求方法和头部
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + DEEPSEEK_API_KEY);
        connection.setDoOutput(true);
        
        // 构建请求体
        String jsonInputString = String.format(
            "{\"model\": \"deepseek-chat\", \"messages\": [{\"role\": \"user\", \"content\": \"%s\"}]}",
            prompt.replace("\"", "\\\"")
        );
        
        // 发送请求
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        // 读取响应
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        
        // 解析JSON响应，提取内容
        // 这里简化处理，实际应使用JSON解析库
        int contentStart = response.indexOf("\"content\":\"") + 11;
        int contentEnd = response.indexOf("\"", contentStart);
        if (contentStart > 10 && contentEnd > contentStart) {
            return response.substring(contentStart, contentEnd);
        } else {
            throw new IOException("无法解析API响应");
        }
    }
    
    /**
     * 显示推荐结果
     */
    private static void displayRecommendations(String response) {
        System.out.println("\n为您推荐以下电视剧：");
        String[] recommendations = response.split("\n");
        
        for (String rec : recommendations) {
            // 提取ID
            String id = "";
            if (rec.contains("ID: ")) {
                int start = rec.indexOf("ID: ") + 4;
                int end = rec.indexOf(",", start);
                if (end > start) {
                    id = rec.substring(start, end).trim();
                }
            }
            
            // 如果找到了ID，显示详细信息
            if (!id.isEmpty()) {
                TVShow show = findTVShowById(id);
                if (show != null) {
                    System.out.println("\n-------------------------------------");
                    displayTVShowDetails(show);
                } else {
                    System.out.println(rec);
                }
            } else {
                System.out.println(rec);
            }
        }
    }
    
    /**
     * 提供备选推荐（当API调用失败时）
     */
    private static void provideFallbackRecommendations(String preferences) {
        // 简单的基于关键词匹配的推荐
        List<TVShow> recommendations = new ArrayList<>();
        String lowerPref = preferences.toLowerCase();
        
        for (TVShow show : tvShows) {
            if (show.getGenre().toLowerCase().contains(lowerPref) ||
                show.getTitle().toLowerCase().contains(lowerPref) ||
                show.getDirector().toLowerCase().contains(lowerPref) ||
                show.getActors().stream().anyMatch(actor -> actor.toLowerCase().contains(lowerPref))) {
                recommendations.add(show);
                if (recommendations.size() >= 5) break;
            }
        }
        
        // 如果没有找到匹配的，推荐热门剧集
        if (recommendations.isEmpty()) {
            List<TVShow> sortedByRating = new ArrayList<>(tvShows);
            sortedByRating.sort((a, b) -> Double.compare(b.getRating(), a.getRating()));
            for (int i = 0; i < Math.min(5, sortedByRating.size()); i++) {
                recommendations.add(sortedByRating.get(i));
            }
        }
        
        // 显示推荐
        for (int i = 0; i < recommendations.size(); i++) {
            System.out.println("\n" + (i + 1) + ".");
            displayTVShowDetails(recommendations.get(i));
        }
    }
    
    /**
     * 显示普通用户菜单
     */
    private static void showUserMenu() {
        System.out.println("\n========== 系统主菜单 ==========");
        System.out.println("1. 查询电视剧");
        System.out.println("2. 评分与评价");
        System.out.println("3. 查看电视剧评价");
        System.out.println("4. 查看热播电视剧榜");
        System.out.println("5. 返回推荐菜单");
        System.out.println("6. 退出登录");
        System.out.print("请选择操作: ");
        
        Scanner scanner = new Scanner(System.in);
        int choice = scanner.nextInt();
        scanner.nextLine(); // 消耗换行符
        
        switch (choice) {
            case 1:
                searchTVShows();
                showUserMenu();
                break;
            case 2:
                rateTVShow();
                showUserMenu();
                break;
            case 3:
                viewTVShowRatings();
                showUserMenu();
                break;
            case 4:
                viewTop10TVShows();
                showUserMenu();
                break;
            case 5:
                // 返回推荐入口菜单
                showPostLoginMenu();
                break;
            case 6:
                currentUser = null;
                System.out.println("已退出登录！");
                break;
            default:
                System.out.println("无效的选择，请重新输入！");
                showUserMenu();
        }
    }
}