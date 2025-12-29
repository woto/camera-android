# Google Play Data Safety Guide for Volley Cam

Use this guide when filling out the "Data Safety" section in the Google Play Console.

## Step 1: Data Collection and Security
- **Does your app collect or share any of the required user data types?** -> **Yes**
- **Is all of the user data collected by your app encrypted in transit?** -> **Yes**
- **Do you provide a way for users to request that their data be deleted?** -> **Yes** (Assuming manual deletion via request or in-app/web feature is possible, otherwise select 'No' but be aware this is less favorable).

## Step 2: Data Types
Select the following data types:

### Photos and videos
- **Videos**: [x] Checked
    - **Collected?**: Yes
    - **Shared?**: Yes (Transferred to server)
    - **Processed ephemerally?**: No
    - **Required or Optional?**: Required (Core functionality)
    - **Why is this data collected?**: App functionality

### Audio files
- **Voice or sound recordings**: [x] Checked
    - **Collected?**: Yes
    - **Shared?**: Yes
    - **Processed ephemerally?**: No
    - **Required or Optional?**: Required
    - **Why is this data collected?**: App functionality

## Step 3: Data Usage and Handling
For both **Videos** and **Audio**:
- **Purposes**: App functionality
- **Data Sharing**: "Data is transferred to a third party" (Your server counts as 'off-device', often misinterpreted, but if you own the server, it's technically "Collected". If you share it with *other* users, it is often safer to declare "Shared" or just "Collected" with specific disclosure. For a custom backend you own, "Collected" is usually sufficient, but since it's "Publicly available to anyone with a code", "Shared" is accurate).

**Advisor Note**: Since you stated "Published clips are available to all internet users", you must be transparent that data is arguably **Shared** publicly.
