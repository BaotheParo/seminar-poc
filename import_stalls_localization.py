import json
import subprocess
import os
import sys

# Support UTF-8 printing on Windows
if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

def import_json_to_db():
    filename = "stalls_localization_export.json"
    if not os.path.exists(filename):
        print(f"Error: {filename} not found.")
        return

    try:
        with open(filename, "r", encoding="utf-8") as f:
            data = json.load(f)
    except Exception as e:
        print(f"Failed to read JSON: {e}")
        return

    print(f"Synchronizing {len(data)} stalls from JSON to Database...")

    for stall in data:
        stall_id = stall['id']
        priority = stall.get('priority', 0)
        translations = stall.get('translations', {})
        
        # 1. Update food_stalls table (core data)
        name_root = stall.get('name', '').replace("'", "''")
        addr_root = stall.get('address', '').replace("'", "''")
        audio_url_vi = f"/audio/{stall_id}_vi.mp3"
        
        sql_stall = f"""
        UPDATE food_stalls 
        SET name = '{name_root}', address = '{addr_root}', audio_url = '{audio_url_vi}', 
            priority = {priority}, localization_status = 'COMPLETE'
        WHERE id = {stall_id};
        """
        subprocess.run(["docker", "exec", "-i", "street-voice-db-new", "psql", "-U", "postgres", "-d", "street_voice_db", "-c", sql_stall], capture_output=True)

        # 2. Update food_stall_localizations table (localized data)
        for lang, content in translations.items():
            name = content.get('name', '').replace("'", "''")
            desc = content.get('description', '').replace("'", "''")
            addr = content.get('address', '').replace("'", "''")
            
            sql_loc = f"""
            INSERT INTO food_stall_localizations (food_stall_id, language_code, name, description, address, created_at)
            VALUES ({stall_id}, '{lang}', '{name}', '{desc}', '{addr}', CURRENT_TIMESTAMP)
            ON CONFLICT (food_stall_id, language_code) 
            DO UPDATE SET 
                name = EXCLUDED.name,
                description = EXCLUDED.description,
                address = EXCLUDED.address;
            """
            subprocess.run(["docker", "exec", "-i", "street-voice-db-new", "psql", "-U", "postgres", "-d", "street_voice_db", "-c", sql_loc], capture_output=True)
            
        print(f"  [v] Synced DB for Stall ID {stall_id}: {stall.get('name')} (Priority: {priority})")

    print("\nDatabase synchronization completed successfully!")

if __name__ == "__main__":
    import_json_to_db()
