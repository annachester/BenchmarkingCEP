#!/usr/bin/env python
# coding: utf-8

# ## Throughput data preprocessing

# In[1]:


import re
import numpy as np
import pandas as pd
import os


# In[2]:


# use for log files, works for pattern and query
def get_throughput_df_from_log(file_path):
    with open(file_path, "r") as file:
        data = file.readlines()
        
    run_ids = []
    worker_ids = []
    elements_per_second = []
    mb_per_sec = []
    
    settings = {}#"file_loops", "vel_thresh", "qua_thresh", "window", "iteration", "throughput", "workers"}
    
    pattern = r"\$(.*?)\$"
    linecount = 0
    runcount = 0
    for line in data:
        
        if "util.ThroughputLogger" in line:
            values = re.findall(pattern, line)
            
            if "file loops" in line: #first line with settings
                settings["name"] = values[0]
                settings["file_loops"] = int(values[1])
                settings["vel_thresh"] = int(values[2])
                if values[3] != "null":
                    settings["qua_thresh"] = int(values[3])
                settings["window"] = int(values[4])
                if values[5] != "null":
                    settings["iteration"] = int(values[5])
            
            elif "sensors" in line: #optional line for parallel execution
                settings["sensors"] = int(values[0])
                
            elif "throughput" in line: #second line
                settings["throughput"] = int(values[0])
                settings["workers"] = int(values[1])
            
            elif "elements/second" in line: #all other lines
                linecount += 1
                #this line may be ueberfluessig
                run_ids.append(runcount)
                worker_ids.append(int(values[0]))
                elements_per_second.append(int(values[1]))
                mb_per_sec.append(float(values[2]))

                if linecount % settings["workers"] == 0:
                    runcount += 1
    df = pd.DataFrame({
        "index": run_ids,
        "Worker ID": worker_ids,
        "Elements/Second": elements_per_second,
        "MB/Second": mb_per_sec
    })
    
    return df


# In[1]:


def get_settings_from_log(file_path):
    with open(file_path, "r") as file:
        data = file.readlines()
        
    settings = {}
    
    for line in data:
        if "util.ThroughputLogger" in line:
            pattern = r"\$(.*?)\$"
            values = re.findall(pattern, line)
            if "file loops" in line: #first line with settings
                settings["name"] = make_nice_name(values[0])
                settings["file_loops"] = int(values[1])
                settings["vel_thresh"] = int(values[2])
                if values[3] != "null":
                    settings["qua_thresh"] = int(values[3])
                settings["window"] = int(values[4])
                if values[5] != "null":
                    settings["iteration"] = int(values[5])
            
            elif "sensors" in line: #optional line for parallel execution
                settings["sensors"] = int(values[0])
                
            elif "throughput" in line: #second line
                settings["throughput"] = int(values[0])
                settings["workers"] = int(values[1])
                
    return settings


# In[19]:


def make_nice_name(name):
    if name.endswith("LS"):
        name = name.split("LS")[0]
        
    if "Pattern" in name:
        nice_name = "FlinkCEP_"
    elif "Query" in name:
        nice_name = "CEP2ASP_"
        
    if "ITER1" in name:
        nice_name += "ITER1"
    elif "ITER2" in name:
        nice_name += "ITER2"
    
    elif "AND" in name:
        nice_name += "AND"
    elif "OR" in name:
        nice_name += "OR"    
    
    elif "Q1_SEQ" in name:
        nice_name += "SEQ1-SP0"
    elif "Q1_1_SEQ" in name:
        nice_name += "SEQ1-SP1"
    elif "Q1_2_SEQ" in name:
        nice_name += "SEQ1-SP2"
        
    elif "Q8_SEQ" in name:
        nice_name += "SEQ2-SP0"
    elif "Q8_1_SEQ" in name:
        nice_name += "SEQ2-SP1"
    elif "Q8_2_SEQ" in name:
        nice_name += "SEQ2-SP2"
    
    return nice_name


# In[20]:


#print(make_nice_name("Q8_2_SEQPatternLS"))


# In[5]:


#use for logs saved to csv, this approach was not used in the end because of slow writing
def get_throughput_df_from_csv(file_path):
    with open(file_path, "r") as file:
        data = file.readlines()


    # Define empty lists to store extracted information
    run_ids = []
    worker_ids = []
    elements_per_second = []
    mb_per_sec = []

    # Regular expression pattern to extract numerical values from the lines surrounded by $
    pattern = r"\$(.*?)\$"

    runcount = 0
    linecount = 0
    data_ingestion_rate = 0
    number_of_workers = 0
    for line in data:
        values = re.findall(pattern, line)

        if not line.startswith("Worker"): #header lines
            data_ingestion_rate = int(values[0])
            number_of_workers = int(values[1])
            
        else:
            if len(values) == 3:
                linecount += 1
                worker_id = int(values[0])
                elements_per_second_value = float(values[1])
                mb_per_sec_value = float(values[2])

                run_ids.append(runcount)
                worker_ids.append(worker_id)
                elements_per_second.append(elements_per_second_value)
                mb_per_sec.append(mb_per_sec_value)

                if linecount % number_of_workers == 0:
                    runcount += 1

    # Create a pandas DataFrame from the extracted data
    df = pd.DataFrame({
        "index": run_ids,
        "Worker ID": worker_ids,
        "Elements/Second": elements_per_second,
        "MB/Second": mb_per_sec
    })
    return df, number_of_workers, data_ingestion_rate


# In[7]:


def get_avg_throughput(df):
    Elements_sec_worker = df["Elements_sec_worker"].mean()
    Total_elements_sec = df["Total_elements_sec"].mean()
    MB_sec_worker = df["MB_sec_worker"].mean()
    return np.array([Elements_sec_worker, Total_elements_sec, MB_sec_worker])


# ## Latency data preprocessing

# In[1]:


def get_latency_df_from_log(file_path):
    with open(file_path, "r") as file:
        data = file.readlines()

    event_det_lat = []
    pattern_det_lat = []
    total_latency = []
    patterns_cnt = []

    # Regular expression pattern to extract numerical values from the lines surrounded by $
    pattern = r"\$(.*?)\$"

    for line in data:
        if "util.LatencyLogger" in line:
            values = re.findall(pattern, line)

            if len(values) == 4: 
                # Append the extracted data to the lists
                event_det_lat.append(int(values[0]))
                pattern_det_lat.append(int(values[1]))
                total_latency.append(int(values[2]))
                patterns_cnt.append(int(values[3]))
#             else:
#                 print(line) #header

    df = pd.DataFrame({
        "event_det_lat": event_det_lat,
        "pattern_det_lat": pattern_det_lat,
        "total_latency": total_latency,
        "patterns_cnt": patterns_cnt
    })
    return df


# In[2]:


#use for logs saved to csv, this approach was not used in the end because of slow writing
#used for CEP2ASP approach, gets unique result, since approach outputs multiple duplicates
def create_latency_df_query_unique(file_path):
    
    latency_path = file_path+"/latency.csv"
    result_path = file_path+"/result_tuples.csv"
    
    with open(latency_path, "r") as file:
        data = file.readlines()

    event_det_lat = []
    pattern_det_lat = []
    total_latency = []

    # Regular expression pattern to extract numerical values from the lines surrounded by $
    pattern = r"\$(.*?)\$"

    for line in data:
        values = re.findall(pattern, line)

        if len(values) == 3:
            event_det_lat.append(int(values[0]))
            pattern_det_lat.append(int(values[1]))
            total_latency.append(int(values[2]))
        else:
            print("expeted three columns!")

    with open(result_path, "r") as file:
        data = file.readlines()

    result_tuples = []
    for line in data:
        result_tuples.append(line)

    df = pd.DataFrame({
        "tuple": result_tuples,
        "event detection latency": event_det_lat,
        "pattern detection latency": pattern_det_lat,
        "total latency": total_latency
    })
    
    #to get unique tuples, it takes mean, but other possibility: take first occurence
    grouped_df = df.groupby("tuple").agg(
        event_det_lat=("event detection latency", "mean"),
        pattern_det_lat=("pattern detection latency", "mean"),
        total_latency=("total latency", "mean")).reset_index()

    return grouped_df


# In[13]:


def get_avg_det_latency(latency_query_unique):
    event_det_lat_sum = latency_query_unique["event_det_lat"].mean()
    pattern_det_lat_sum = latency_query_unique["pattern_det_lat"].mean()
    total_latency_sum = latency_query_unique["total_latency"].mean()
    return np.array([event_det_lat_sum, pattern_det_lat_sum, total_latency_sum])


# ### Merged processing

# In[5]:


# used for "selecivity" experiments:
# merges all information into one dataframe, result columns:
# Elements/Second, Latency, System, Pattern, Selectivity
def get_merged_df_per_pattern_sel(path, sel_folders):
    merge_df = pd.DataFrame()
    for sel in sel_folders:
        file_path = path+"/"+sel
        merge_df2 = pd.DataFrame()
        for file_name in os.listdir(file_path):
            if os.path.isfile(os.path.join(file_path, file_name)):
                file = file_path + "/" + file_name
                tput_df = get_throughput_df_from_log(file)#.rename(columns={"Elements/Second": "Throughput"})
                tput_df = tput_df["Elements/Second"]/1000 #display for k elems
                lat_df = get_latency_df_from_log(file)
                latency = lat_df["total_latency"]/lat_df["patterns_cnt"]
                if latency.mean()>10000: #something went wrong
                    lat_df["Latency"] = np.nan
                else:
                    lat_df["Latency"] = latency
                settings = get_settings_from_log(file)
                system, pattern = settings["name"].split("_")
                df = pd.concat([tput_df, lat_df], axis=1)[["Elements/Second", "Latency"]].assign(System=system, Pattern=pattern)
                merge_df2 = merge_df2.append(df)
        merge_df2 = merge_df2.assign(Selectivity=sel)
        merge_df = merge_df.append(merge_df2)
    
    return merge_df.reset_index(drop=True)


# In[6]:


# used for "all" experiments:
# merges all information into one dataframe, result columns:
# Elements/Second, Latency, System, Pattern
def get_merged_df_all(path, folders):
    merge_df = pd.DataFrame()
    folder_is_ingestion_rate=False
    if folders[0]=="200k": #very hard coded, change for other labbeling or other start rate
        folder_is_ingestion_rate=True
    for folder in folders:
        file_path = path+"/"+folder
        merge_df2 = pd.DataFrame()
        for file_name in os.listdir(file_path):
            if os.path.isfile(os.path.join(file_path, file_name)):
                file = file_path + "/" + file_name
                tput_df = get_throughput_df_from_log(file)#.rename(columns={"Elements/Second": "Throughput"})
                tput_df = tput_df["Elements/Second"]/1000
                lat_df = get_latency_df_from_log(file)
                latency = lat_df["total_latency"]/lat_df["patterns_cnt"]
                if latency.mean()>10000: #something went wrong
                    lat_df["Latency"] = np.nan
                else:
                    lat_df["Latency"] = latency
                settings = get_settings_from_log(file)
                system, pattern = settings["name"].split("_")
                df = pd.concat([tput_df, lat_df], axis=1)[["Elements/Second", "Latency"]].assign(System=system, Pattern=pattern)
                if folder_is_ingestion_rate:
                    df["ingestion_rate"] = folder
                merge_df2 = merge_df2.append(df)
        merge_df = merge_df.append(merge_df2)
    
    return merge_df.reset_index(drop=True)

