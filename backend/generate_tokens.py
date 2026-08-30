from users.models import User
from rest_framework_simplejwt.tokens import RefreshToken

tokens = []
for i in range(200):
    phone = f"09{50000000 + i}"
    user, _ = User.objects.get_or_create(
        phone=phone, defaults={"username": phone, "full_name": f"LoadTest {i}", "role": "buyer"}
    )
    tokens.append(str(RefreshToken.for_user(user).access_token))

with open("tokens.txt", "w") as f:
    f.write("\n".join(tokens))
print(f"{len(tokens)} tokens written to tokens.txt")