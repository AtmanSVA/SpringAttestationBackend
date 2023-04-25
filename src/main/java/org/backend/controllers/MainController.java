package org.backend.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.backend.models.Cart;
import org.backend.models.Order;
import org.backend.models.Product;
import org.backend.models.User;
import org.backend.repos.CartRepo;
import org.backend.repos.CategoryRepo;
import org.backend.repos.OrderRepo;
import org.backend.security.UserDetails;
import org.backend.services.ProductService;
import org.backend.services.UserService;
import org.backend.util.Validators;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
public class MainController {
    private final UserService userService;
    private final ProductService productService;

    private final CategoryRepo categoryRepo;
    private final CartRepo cartRepo;
    private final OrderRepo orderRepo;

    public MainController(UserService userService, ProductService productService, CategoryRepo categoryRepo, CartRepo cartRepo, OrderRepo orderRepo) {
        this.userService = userService;
        this.productService = productService;
        this.categoryRepo = categoryRepo;
        this.cartRepo = cartRepo;
        this.orderRepo = orderRepo;
    }

    @GetMapping("/")
    public String index(@RequestParam(value = "search", required = false) String search,
                        @RequestParam(value = "category", required = false) String category,
                        @RequestParam(value = "price_from", required = false) String priceFrom,
                        @RequestParam(value = "price_to", required = false) String priceTo,
                        @RequestParam(value = "sort_by", required = false) String sortBy,
                        Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth.getPrincipal() instanceof UserDetails userDetails) {
            System.out.println(userDetails.getUser().getRole());
            model.addAttribute("user", userDetails.getUser());
        }

        int minPrice = -1, maxPrice = -1;

        System.out.println(priceFrom);

        if (priceFrom != null && !priceFrom.isEmpty())
            minPrice = Integer.parseInt(priceFrom);

        if (priceTo != null && !priceTo.isEmpty())
            maxPrice = Integer.parseInt(priceTo);

        var productsList = productService.findByFilters(search, categoryRepo.findByName(category), minPrice, maxPrice, sortBy);

        model.addAttribute("products", productsList);
        model.addAttribute("categories", categoryRepo.findAll());

        return "index";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        return "redirect:/login?logout";
    }

    @GetMapping("/registration")
    public String regPage(@ModelAttribute("user") User user) {
        return "registration";
    }

    @PostMapping("/registration")
    public String regResult(@ModelAttribute("user") User user, @RequestParam("password_confirm") String passConfirm, Errors errors) {

        user.setUsername(user.getUsername().toLowerCase());

        if (userService.getByUsername(user.getUsername()) != null) {
            errors.rejectValue("username", "", "Имя пользователя занято! Попробуйте другой");
            return "registration";
        }

        if (!Validators.validateUsername(user.getUsername()))
            errors.rejectValue("username", "", "Невалидное имя пользователя!");

        if (!Validators.validatePassword(user.getPassword()))
            errors.rejectValue("password", "", "Невалидный пароль!");

        if (!user.getPassword().equals(passConfirm))
            errors.reject("password_confirm", "Пароли не совпадают!");

        if (errors.hasErrors()) {
            return "registration";
        }

        userService.register(user);
        return "login";
    }

    @GetMapping("/product/{id}")
    public String infoProduct(@PathVariable("id") int id, Model model){
        model.addAttribute("product", productService.getProductId(id));
        return "product";
    }

    @GetMapping("/403")
    public String accessDenied() {
        return "403";
    }

    @GetMapping("/cart_add/{id}")
    public String addProductInCart(@PathVariable("id") int id, Model model){
        Product product = productService.getProductId(id);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails personDetails = (UserDetails) authentication.getPrincipal();
        int id_person = personDetails.getUser().getId();

        Cart cart = cartRepo.findByUserId(id_person);

        if (cart == null) {
            cart = new Cart();
            cart.setUser(personDetails.getUser());
        }

        cart.setUser(personDetails.getUser());
        cart.addProduct(productService.getProductId(id));

        cartRepo.save(cart);
        return "redirect:/cart";
    }

    @GetMapping("/cart")
    public String cart(Model model){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        int id_person = userDetails.getUser().getId();
        Cart cart = cartRepo.findByUserId(id_person);
        if (cart == null) {
            cart = new Cart();
            cart.setUser(userDetails.getUser());
        }

        List<Product> productList = cart.getProducts();

        float price = 0;
        for (Product product: productList) {
            price += product.getPrice();
        }

        model.addAttribute("user", userDetails.getUser());
        model.addAttribute("price", price);
        model.addAttribute("cart_product", productList);
        return "cart";
    }

    @GetMapping("/cart_delete/{product_idx}")
    public String deleteProductFromCart(Model model, @PathVariable("product_idx") int index) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        Cart cart = cartRepo.findByUserId(user.getId());

        if (index >= 0 && index < cart.getProducts().size()) {
            cart.getProducts().remove(index);
            cartRepo.save(cart);
        }

        return "redirect:/cart";
    }

    @GetMapping("/create_order")
    public String order() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails personDetails = (UserDetails) authentication.getPrincipal();
        int id_person = personDetails.getUser().getId();

        Cart cart = cartRepo.findByUserId(id_person);
        List<Product> productList = cart.getProducts();

        float price = 0;
        for (Product product: productList) {
            price += product.getPrice();
        }

        String uuid = UUID.randomUUID().toString();

        for (Product product : productList) {
            Order newOrder = new Order(uuid, product, personDetails.getUser(), 1, product.getPrice(), "Оформлен");
            orderRepo.save(newOrder);
        }

        cart.getProducts().clear();
        cartRepo.save(cart);

        return "redirect:/orders";
    }

    @GetMapping("/orders")
    public String orderUser(Model model){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        List<Order> orderList = orderRepo.findByUser(userDetails.getUser());
        model.addAttribute("orders", orderList);
        model.addAttribute("user", userDetails.getUser());
        return "orders";
    }

    @GetMapping("/test/panel")
    public String testPanel(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        List<Order> orderList = orderRepo.findByUser(userDetails.getUser());
        model.addAttribute("orders", orderList);
        model.addAttribute("user", userDetails.getUser());
        return "test/panel";
    }
}
