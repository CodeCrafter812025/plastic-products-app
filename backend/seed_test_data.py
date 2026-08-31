from users.models import User
from products.models import Product

admin, created = User.objects.get_or_create(
    phone="09120000001",
    defaults={"full_name": "Admin Test", "role": "admin", "username": "09120000001"}
)
if admin.role != "admin":
    admin.role = "admin"
    admin.save()

product, created = Product.objects.get_or_create(
    title="Test Product",
    defaults={
        "price": 150000,
        "weight": 1,
        "color": "blue",
        "quality": "primary",
        "stock": 100,
        "created_by": admin,
    }
)
print("PRODUCT_ID:", product.id)
visitor, _ = User.objects.get_or_create(
    phone="09130000002",
    defaults={"full_name": "Visitor Test", "role": "visitor", "username": "09130000002"}
)
print("VISITOR_ID:", visitor.id)