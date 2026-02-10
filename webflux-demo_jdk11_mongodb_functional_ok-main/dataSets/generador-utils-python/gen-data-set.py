import csv
import time
import random

def generate_product_doc_csv(filename, count):
    with open(filename, 'w', newline='') as csvfile:
        writer = csv.writer(csvfile)
        writer.writerow(['sku', 'name', 'price', 'ts'])

        current_ts = int(time.time())
        for i in range(1, count + 1):
            sku = f"SKU-{i:07d}"
            name = f"Producto Masivo {i}"
            price = round(random.uniform(10.0, 5000.0), 2)
            writer.writerow([sku, name, price, current_ts])

# Generar 1,000,000 de registros
generate_product_doc_csv('bulk_load_test.csv', 1000000)
print("Archivo CSV generado exitosamente.")