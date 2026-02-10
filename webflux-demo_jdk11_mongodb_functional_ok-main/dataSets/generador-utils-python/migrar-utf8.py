import csv
from datetime import datetime, timezone

def generate_utf8_dataset(filename, count):
    # Abrimos con utf-8-sig para máxima compatibilidad
    with open(filename, 'w', newline='', encoding='utf-8-sig') as f:
        writer = csv.writer(f)
        writer.writerow(['nombre', 'categoria', 'precio'])

        for i in range(1, count + 1):
            # Ejemplo con caracteres especiales para probar la codificación
            writer.writerow([f"Producto Edición Especial {i}", "Computación", 100.0])

generate_utf8_dataset('bulk_productos_utf8.csv', 1000000)