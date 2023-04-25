package org.backend.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.backend.models.Category;
import org.backend.models.Product;
import org.backend.repos.ProductRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepo productRepo;

    @PersistenceContext
    private EntityManager entityManager;

    public ProductService(ProductRepo productRepo) {
        this.productRepo = productRepo;
    }

    public List<Product> getAllProduct(){
        return productRepo.findAll();
    }

    public Product getProductId(int id){
        Optional<Product> optionalProduct = productRepo.findById(id);
        return optionalProduct.orElse(null);
    }

    @Transactional
    public void saveProduct(Product product, Category category){
        product.setCategory(category);
        productRepo.save(product);
    }

    @Transactional
    public void updateProduct(int id, Product product){
        product.setId(id);
        productRepo.save(product);
    }

    @Transactional
    public void deleteProduct(int id){
        productRepo.deleteById(id);
    }

    public List<Product> findByFilters(String name, Category category, int minPrice, int maxPrice, String order) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Product> query = cb.createQuery(Product.class);
        Root<Product> root = query.from(Product.class);

        List<Predicate> predicates = new ArrayList<>();

        if (name != null && !name.isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("title")), "%" + name.toLowerCase() + "%"));
        }

        if (category != null) {
            predicates.add(cb.equal(root.get("category"), category));
        }

        if (minPrice != -1) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
        }

        if (maxPrice != -1) {
            predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
        }

        if (order != null && !order.isEmpty()) {
            if (order.equals("price_asc")) {
                query.orderBy(cb.asc(root.get("price")));
            } else if (order.equals("price_desc")) {
                query.orderBy(cb.desc(root.get("price")));
            }
        }


        query.where(predicates.toArray(new Predicate[0]));

        return entityManager.createQuery(query).getResultList();
    }
}
